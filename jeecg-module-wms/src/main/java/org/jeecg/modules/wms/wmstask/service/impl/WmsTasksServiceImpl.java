package org.jeecg.modules.wms.wmstask.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.DateUtils;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrderItems;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrders;
import org.jeecg.modules.wms.inorder.service.IWmsStockInOrderItemsService;
import org.jeecg.modules.wms.inorder.service.IWmsStockInOrdersService;
import org.jeecg.modules.wms.inorder.service.impl.WmsStockInOrdersServiceImpl;
import org.jeecg.modules.wms.wmstask.entity.WmsTasks;
import org.jeecg.modules.wms.wmstask.mapper.WmsTasksMapper;
import org.jeecg.modules.wms.wmstask.service.IWmsTasksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @Description: 任务表
 * @Author: jeecg-boot
 * @Date:   2026-09-20
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReceviceTask(String orderId, String operator) {
        // 查询入库单
        WmsStockInOrders wmsStockInOrders = wmsStockInOrdersService.getById(orderId);

        // 检验入库单的状态为审核通过 方可创建收获任务
        if(!wmsStockInOrders.getStatus().equals(WarehouseDictEnum.INBOUND_APPROVED.getCode())) {
            throw new RuntimeException("只有审核通过的入库单才能创建收获任务");
        }

        // 查询入库单明细
        List<WmsStockInOrderItems> wmsStockInOrderItems = wmsStockInOrderItemsService.selectByMainId(orderId);

        // 遍历入库单明细，创建收获任务
        for(WmsStockInOrderItems entity : wmsStockInOrderItems) {
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
            if(!save) {
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
        /**
         * 生成任务编号
         * 规则: TSK+年月日+5位序号，序号使用redis自增序号实现
         */
        public String generateTaskCode() {
            //参考上边的代码实现
            String time = DateUtils.now().substring(0, 10).replace("-", "");
            String key = "tsk_number"+time;
            long incr = redisUtil.incr(key, 1);
            if(incr == 1){
                //设置过期时间，设置24小时+10秒的目的是避免并发产生订单号重复
                redisUtil.expire(key, 24*60*60+10);
            }
            //将incr组成4位字符串
            String incrStr = String.format("%05d", incr);
            String taskNumber = "TSK"+time+incrStr;

            return taskNumber;
        }
    }

