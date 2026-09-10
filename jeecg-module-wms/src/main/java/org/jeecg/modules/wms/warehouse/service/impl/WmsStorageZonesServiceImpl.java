package org.jeecg.modules.wms.warehouse.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.jeecg.modules.wms.warehouse.entity.WmsStorageZones;
import org.jeecg.modules.wms.warehouse.mapper.WmsStorageZonesMapper;
import org.jeecg.modules.wms.warehouse.service.IWmsStorageZonesService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;

/**
 * @Description: 储区表
 * @Author: jeecg-boot
 * @Date:   2026-09-06
 * @Version: V1.0
 */
@Service
public class WmsStorageZonesServiceImpl extends ServiceImpl<WmsStorageZonesMapper, WmsStorageZones> implements IWmsStorageZonesService {

    @Override
    public IPage<WmsStorageZones> queryList(WmsStorageZones wmsStorageZones, Integer pageNo, Integer pageSize) {

        // 向 threadLocal 中设置分页参数
        Page<WmsStorageZones> objects = PageHelper.startPage(pageNo, pageSize);
        // 当执行 mapper 时被 pageInterceptor 拦截器拦截，向原始sql后拼接 limit ?,?
        List<WmsStorageZones> wmsStorageZones1 = getBaseMapper().queryList(wmsStorageZones);

        PageDTO<WmsStorageZones> wmsStorageZonesPageDTO = new PageDTO<>();
        wmsStorageZonesPageDTO.setRecords(wmsStorageZones1); // 当前页的数据
        wmsStorageZonesPageDTO.setTotal(objects.getTotal()); // 总记录数
        wmsStorageZonesPageDTO.setPages(objects.getPages()); // 总页数
        wmsStorageZonesPageDTO.setCurrent(pageNo); // 当前页码
        wmsStorageZonesPageDTO.setSize(pageSize); // 每页记录数

        return wmsStorageZonesPageDTO;
    }
}
