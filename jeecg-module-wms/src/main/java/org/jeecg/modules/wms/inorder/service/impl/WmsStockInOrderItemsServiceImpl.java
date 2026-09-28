package org.jeecg.modules.wms.inorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrderItems;
import org.jeecg.modules.wms.inorder.mapper.WmsStockInOrderItemsMapper;
import org.jeecg.modules.wms.inorder.service.IWmsStockInOrderItemsService;
import org.jeecg.modules.wms.wmstask.entity.WmsTasksRecords;
import org.jeecg.modules.wms.wmstask.service.IWmsTasksRecordsService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description: 入库单明细
 * @Author: jeecg-boot
 * @Date: 2026-09-14
 * @Version: V1.0
 */
@Service
public class WmsStockInOrderItemsServiceImpl extends ServiceImpl<WmsStockInOrderItemsMapper, WmsStockInOrderItems> implements IWmsStockInOrderItemsService {

    @Autowired
    private WmsStockInOrderItemsMapper wmsStockInOrderItemsMapper;

    // 收货记录service
    @Autowired
    private IWmsTasksRecordsService wmsTasksRecordsService;

    @Override
    public List<WmsStockInOrderItems> selectByMainId(String mainId) {
        return wmsStockInOrderItemsMapper.selectByMainId(mainId);
    }

    @Override
    public void updateReceivedStatus(String stockInOrderItemId) {
        // 查询入库单明细
        WmsStockInOrderItems wmsStockInOrderItems = getById(stockInOrderItemId);

        // 更新入库单明细中的收货数量以及不良品数量、状态

        // 查询入库单明细对应的收费记录（多个），从中找到总共收货多少良品、不良品
        LambdaUpdateWrapper<WmsTasksRecords> eq = new LambdaUpdateWrapper<WmsTasksRecords>()
                .eq(WmsTasksRecords::getStockInOrderItemId, stockInOrderItemId);
        List<WmsTasksRecords> wmsTasksRecords = wmsTasksRecordsService.list(eq);

        // 汇总 wmsTasksRecords 中的不良品

        // 找到不良品数量
        int badQuantity = wmsTasksRecords.stream()
                .filter(item -> item.getInventoryAttribute().equals(WarehouseDictEnum.INVENTORY_ATTRIBUTE_DEFECTIVE.getCode()))
                .mapToInt(WmsTasksRecords::getExecQuantity).sum();

        // 汇总良品数量
        int goodQuantity = wmsTasksRecords.stream()
                .filter(item -> item.getInventoryAttribute().equals(WarehouseDictEnum.INVENTORY_ATTRIBUTE_GOOD.getCode()))
                .mapToInt(WmsTasksRecords::getExecQuantity).sum();

        wmsStockInOrderItems.setReceivedQuantity(goodQuantity); // 实际良品数量
        wmsStockInOrderItems.setReceivedQuantity(badQuantity); // 实际不良品数量
        // 如果良品数量加不良品数量等于采购数量，状态为收货完成
        if(goodQuantity + badQuantity == wmsStockInOrderItems.getExpectedQuantity()){
            wmsStockInOrderItems.setStatus(WarehouseDictEnum.INBOUND_DETAIL_RECEIVED.getCode());
        }
        updateById(wmsStockInOrderItems);
    }
}
