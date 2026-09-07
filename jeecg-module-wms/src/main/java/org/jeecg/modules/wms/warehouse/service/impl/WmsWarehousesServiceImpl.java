package org.jeecg.modules.wms.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.warehouse.entity.WmsWarehouses;
import org.jeecg.modules.wms.warehouse.mapper.WmsWarehousesMapper;
import org.jeecg.modules.wms.warehouse.service.IWmsWarehousesService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


/**
 * @Description: 仓库表
 * @Author: jeecg-boot
 * @Date:   2026-09-06
 * @Version: V1.0
 */
@Service
public class WmsWarehousesServiceImpl extends ServiceImpl<WmsWarehousesMapper, WmsWarehouses> implements IWmsWarehousesService {


    @Override
    public void add(WmsWarehouses wmsWarehouses) {
        String warehouseCode = wmsWarehouses.getWarehouseCode();
        // 校验仓库代码唯一性
        // 根据仓库代码查询数据库的仓库表，如果存在记录说明仓库代码已经存在
        // sql = select count(1) from wms_warehouses where warehouse_code=?
        LambdaQueryWrapper<WmsWarehouses> eq = new LambdaQueryWrapper<WmsWarehouses>()
                .eq(WmsWarehouses::getWarehouseCode,warehouseCode);
        long count = this.count(eq);
        if (count > 0) {
            throw new JeecgBootException("仓库代码已经存在");
        }
        // 仓库状态默认为“创建”
        wmsWarehouses.setStatus(WarehouseDictEnum.STATUS_CREATED.getCode());
        this.save(wmsWarehouses);
    }

    @Override
    public void edit(WmsWarehouses wmsWarehouses) {
        String warehouseCode = wmsWarehouses.getWarehouseCode();
        // 校验仓库代码唯一性
        // 根据仓库代码查询数据库的仓库表，如果存在记录说明仓库代码已经存在
        // sql = select count(1) from wms_warehouses where warehouse_code=? and id != ?
        LambdaQueryWrapper<WmsWarehouses> eq = new LambdaQueryWrapper<WmsWarehouses>()
                .eq(WmsWarehouses::getWarehouseCode,warehouseCode)
                .ne(WmsWarehouses::getId,wmsWarehouses.getId());
        long count = this.count(eq);
        if (count > 0) {
            throw new JeecgBootException("仓库代码已经存在");
        }
        this.updateById(wmsWarehouses);
    }

    @Override
    public void enable(String id) {
        // 根据id查询仓库
        WmsWarehouses wmsWarehouses = this.getById(id);
        // 如果仓库不存在
        if (wmsWarehouses == null) {
            throw new JeecgBootException("仓库不存在");
        }
        // 拿到仓库状态
        String status = wmsWarehouses.getStatus();
        // 仓库状态位“创建”或禁用时方可启用
        if (!(status.equals(WarehouseDictEnum.STATUS_CREATED.getCode()) || status.equals(WarehouseDictEnum.STATUS_INACTIVE.getCode()))) {
            throw new JeecgBootException("仓库状态位“创建”或禁用时方可启用");
        }
        // 将仓库状态更新为启用
        wmsWarehouses.setStatus(WarehouseDictEnum.STATUS_ACTIVE.getCode());
        this.updateById(wmsWarehouses);
    }

    @Override
    public void disable(String id) {
        // 根据id查询仓库
        WmsWarehouses wmsWarehouses = this.getById(id);
        // 如果仓库不存在
        if (wmsWarehouses == null) {
            throw new JeecgBootException("仓库不存在");
        }
        // 拿到仓库状态
        String status = wmsWarehouses.getStatus();
        // 仓库状态位“启用”时方可禁用
        if (!(status.equals(WarehouseDictEnum.STATUS_ACTIVE.getCode()))) {
            throw new JeecgBootException("仓库状态位“启用”时方可禁用");
        }
        // 将仓库状态更新为禁用
        wmsWarehouses.setStatus(WarehouseDictEnum.STATUS_INACTIVE.getCode());
        this.updateById(wmsWarehouses);
    }
}
