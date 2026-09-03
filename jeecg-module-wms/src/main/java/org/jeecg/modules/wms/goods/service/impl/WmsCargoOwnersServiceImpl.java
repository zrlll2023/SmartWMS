package org.jeecg.modules.wms.goods.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.wms.goods.entity.WmsCargoOwners;
import org.jeecg.modules.wms.goods.mapper.WmsCargoOwnersMapper;
import org.jeecg.modules.wms.goods.service.IWmsCargoOwnersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 货主表
 * @Author: jeecg-boot
 * @Date:   2026-09-03
 * @Version: V1.0
 */
@Service
public class WmsCargoOwnersServiceImpl extends ServiceImpl<WmsCargoOwnersMapper, WmsCargoOwners>
        implements IWmsCargoOwnersService {

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void add(WmsCargoOwners wmsCargoOwners){
        // 货主编码： C+5位序号   使用redis的自增命令incr 实现
        String code = generateOwnerCode();
        wmsCargoOwners.setOwnerCode(code);
        save(wmsCargoOwners);
    }

    // 生成货主编码
    public String generateOwnerCode() {
        long code = 0;
        try {
            code = redisUtil.incr("WMS_CARGO_OWNERS_CODE", 1);
            String ownerCode = "C"+String.format("%05d", code);
            return ownerCode;
        } catch (Exception e) {
            throw new JeecgBootException("生成货主编码出错");
        }

    }

}
