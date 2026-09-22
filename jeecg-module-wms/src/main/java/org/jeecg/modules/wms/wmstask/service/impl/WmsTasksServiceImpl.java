package org.jeecg.modules.wms.wmstask.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.DateUtils;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrderItems;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrders;
import org.jeecg.modules.wms.inorder.service.IWmsStockInOrderItemsService;
import org.jeecg.modules.wms.inorder.service.IWmsStockInOrdersService;
import org.jeecg.modules.wms.inorder.service.impl.WmsInventoryTransByReceiving;
import org.jeecg.modules.wms.inorder.service.impl.WmsStockInOrdersServiceImpl;
import org.jeecg.modules.wms.inventory.vo.WmsInventoryTransParam;
import org.jeecg.modules.wms.wmstask.entity.WmsTasks;
import org.jeecg.modules.wms.wmstask.entity.WmsTasksRecords;
import org.jeecg.modules.wms.wmstask.mapper.WmsTasksMapper;
import org.jeecg.modules.wms.wmstask.service.IWmsTasksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.jeecg.modules.wms.wmstask.service.IWmsTasksRecordsService; // 新增：任务执行记录的数据访问服务

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @Description: 任务表
 * @Author: jeecg-boot
 * @Date: 2026-09-20
 * @Version: V1.0
 */
@Service
public class WmsTasksServiceImpl extends ServiceImpl<WmsTasksMapper, WmsTasks> implements IWmsTasksService {

    @Autowired
    private IWmsStockInOrdersService wmsStockInOrdersService;

    @Autowired
    private IWmsStockInOrderItemsService wmsStockInOrderItemsService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private WmsInventoryTransByReceiving wmsInventoryTransByReceiving;

    @Autowired
    private IWmsTasksRecordsService wmsTasksRecordsService; // 新增：保存每次收货的任务执行记录

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReceviceTask(String orderId, String operator) {
        // 查询入库单
        WmsStockInOrders wmsStockInOrders = wmsStockInOrdersService.getById(orderId);

        // 检验入库单的状态为审核通过 方可创建收获任务
        if (!wmsStockInOrders.getStatus().equals(WarehouseDictEnum.INBOUND_APPROVED.getCode())) {
            throw new RuntimeException("只有审核通过的入库单才能创建收获任务");
        }

        // 查询入库单明细
        List<WmsStockInOrderItems> wmsStockInOrderItems = wmsStockInOrderItemsService.selectByMainId(orderId);

        // 遍历入库单明细，创建收获任务
        for (WmsStockInOrderItems entity : wmsStockInOrderItems) {
            //创建收货任务
            WmsTasks wmsTasks = new WmsTasks();
            //任务编号
            wmsTasks.setTaskNumber(generateTaskCode());
            //任务类型
            wmsTasks.setTaskType(WarehouseDictEnum.TASK_TYPE_RECEIVING.getCode());
            //任务状态
            wmsTasks.setTaskStatus(WarehouseDictEnum.TASK_STATUS_CREATED.getCode());
            //任务创建时间
            wmsTasks.setCreateTime(new Date());
            //商品id
            wmsTasks.setProductId(entity.getProductId());
            //商品采购数量
            wmsTasks.setQuantity(entity.getExpectedQuantity());
            //完成数量为0
            wmsTasks.setCompletedQuantity(0);
            //执行人
            wmsTasks.setOperator(operator);
            //创建任务
            boolean save = save(wmsTasks);
            if (!save) {
                throw new RuntimeException("创建收获任务失败");
            }
        }

        // 更新入库单的状态为收货中
        WmsStockInOrders stockInOrdersUpdate = new WmsStockInOrders();
        stockInOrdersUpdate.setId(orderId);
        stockInOrdersUpdate.setStatus(WarehouseDictEnum.INBOUND_RECEIVING.getCode());
        wmsStockInOrdersService.updateById(stockInOrdersUpdate);

        // 更新入库单明细的状态为收货中
        LambdaUpdateWrapper<WmsStockInOrderItems> set = new LambdaUpdateWrapper<WmsStockInOrderItems>()
                .eq(WmsStockInOrderItems::getOrderId, orderId)
                .set(WmsStockInOrderItems::getStatus, WarehouseDictEnum.INBOUND_DETAIL_RECEIVING.getCode());
        wmsStockInOrderItemsService.update(set);

    }

    @Override
    public IPage<WmsTasks> list(WmsTasks wmsTasks, Integer pageNo, Integer pageSize) {
        Page<WmsTasks> PageResult = PageHelper.startPage(pageNo, pageSize);
        // 调用mapper
        List<WmsTasks> list = baseMapper.queryTaskList(wmsTasks);
        PageDTO<WmsTasks> wmsTasksPageDTO = new PageDTO();
        wmsTasksPageDTO.setRecords(list);
        wmsTasksPageDTO.setTotal(PageResult.getTotal());
        wmsTasksPageDTO.setSize(pageSize);
        wmsTasksPageDTO.setCurrent(pageNo);
        wmsTasksPageDTO.setPages(PageResult.getPages());
        return wmsTasksPageDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recevice(WmsTasksRecords wmsTasksRecords) {
        // 执行任务 添加任务记录 在任务表记录完成数量 更新任务状态 此方法是一个公共方法用于上架、收货、拣货
        WmsTasks wmsTasks = execute(wmsTasksRecords);

        // 入库单明细id
        String stockInOrderItemId = wmsTasks.getStockInOrderItemId();

        // 更新入库单明细的收获数量、不良品数量及状态
        wmsStockInOrderItemsService.updateReceivedStatus(stockInOrderItemId);

        //更新入库单中的总收获数量、总不良品数量、状态
        wmsStockInOrdersService.updateReceivedStatus(wmsTasks.getStockInOrderId());

        // 存储库存
        WmsInventoryTransParam inventoryTransParam = new WmsInventoryTransParam();
        inventoryTransParam.setProductId(wmsTasks.getProductId()); // 商品id
        inventoryTransParam.setExecQuantity(wmsTasksRecords.getExecQuantity()); // 执行数量
        inventoryTransParam.setWarehouseId(wmsTasks.getSourceWarehouseId());
        inventoryTransParam.setTargetLocationCode(wmsTasksRecords.getTargetLocationCode()); // 目标储位编码
        inventoryTransParam.setSourceLocationCode(wmsTasks.getSourceLocationCode());
        inventoryTransParam.setBatchNumber(wmsTasksRecords.getBatchNumber());
        inventoryTransParam.setExecQuantity(wmsTasksRecords.getExecQuantity());
        // 根据库存属性确定是否可售，良品是可售，不良品为不可售
        inventoryTransParam.setIsSellable(wmsTasksRecords.getInventoryAttribute().equals(WarehouseDictEnum.INVENTORY_ATTRIBUTE_GOOD.getCode()) ? "1" : "0");
        inventoryTransParam.setTransactionType(WarehouseDictEnum.INVENTORY_RECEIVING.getCode()); // 库存变更类型
        inventoryTransParam.setOperator(wmsTasksRecords.getOperator());
        inventoryTransParam.setOperationTime(new Date());

        wmsInventoryTransByReceiving.transfer(inventoryTransParam);

        //todo 如果入库单收货完成自动创建上架任务

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WmsTasks execute(WmsTasksRecords wmsTasksRecords) {
        //任务id
        String taskId = wmsTasksRecords.getTaskId();
        //查询任务信息
        WmsTasks wmsTasks = this.getById(taskId);
        if (wmsTasks == null) {
            throw new RuntimeException("任务不存在");
        }
        // 计划数量
        Integer planQuantity = wmsTasks.getQuantity();
        //已完成数量
        Integer completedQuantity = wmsTasks.getCompletedQuantity();
        //本次执行数量
        Integer execQuantity = wmsTasksRecords.getExecQuantity();
        // 如果完成数量加上本次执行数量大于计划数量则不能执行
        if (completedQuantity + execQuantity > wmsTasks.getQuantity()) {
            throw new RuntimeException("执行数量不能大于计划数量");
        }
        // 向任务记录表填充数据
        //执行人
        wmsTasksRecords.setOperator(wmsTasks.getOperator());
        //执行时间
        wmsTasksRecords.setOperationTime(new Date());
        //入库单id
        wmsTasksRecords.setStockInOrderId(wmsTasks.getStockInOrderId());
        //入库单明细id
        wmsTasksRecords.setStockInOrderItemId(wmsTasks.getStockInOrderItemId());
        //波次id 用于波次管理
        wmsTasksRecords.setWaveOrderId(wmsTasks.getWaveOrderId());
        //来源仓库
        wmsTasksRecords.setSourceWarehouseId(wmsTasks.getSourceWarehouseId());
        //目的仓库
        wmsTasksRecords.setTargetWarehouseId(wmsTasks.getTargetWarehouseId());

        //添加执行任务记录
        boolean save = wmsTasksRecordsService.save(wmsTasksRecords);
        if (!save) {
            throw new RuntimeException("添加任务执行记录失败!");
        }

        //更新任务表中的完成数量,新的完成数量为原有完成数量加收货记录的完成数量
        //        String sql = "update wms_tasks set completed_quantity = completed_quantity + #{ExecQuantity } where id = #{taskId} and completed_quantity <= quantity-#{ExecQuantity }";
        LambdaUpdateWrapper<WmsTasks> wmsTasksLambdaUpdateWrapper = new LambdaUpdateWrapper<WmsTasks>()
                .eq(WmsTasks::getId, taskId)
                .setSql("completed_quantity = completed_quantity + " + wmsTasksRecords.getExecQuantity())
                .le(WmsTasks::getCompletedQuantity, planQuantity - execQuantity);
        boolean update = this.update(null, wmsTasksLambdaUpdateWrapper);
        if (!update) {
            throw new RuntimeException("执行数量不能大于计划数量!");
        }
        //如果完成数量等于计划数量,更新任务状态为已完成
        //查询新的任务信息
        wmsTasks = this.getById(taskId);
        //完成数量
        completedQuantity = wmsTasks.getCompletedQuantity();
        if (completedQuantity >= planQuantity) {
            //更新任务状态为已完成
            wmsTasks.setTaskStatus(WarehouseDictEnum.TASK_STATUS_COMPLETED.getCode());
            this.updateById(wmsTasks);
        }
        return wmsTasks;

    }

    /**
     * 生成任务编号
     * 规则: TSK+年月日+5位序号，序号使用redis自增序号实现
     */
    public String generateTaskCode() {
        //参考上边的代码实现
        String time = DateUtils.now().substring(0, 10).replace("-", "");
        String key = "tsk_number" + time;
        long incr = redisUtil.incr(key, 1);
        if (incr == 1) {
            //设置过期时间，设置24小时+10秒的目的是避免并发产生订单号重复
            redisUtil.expire(key, 24 * 60 * 60 + 10);
        }
        //将incr组成4位字符串
        String incrStr = String.format("%05d", incr);
        String taskNumber = "TSK" + time + incrStr;

        return taskNumber;
    }
}

