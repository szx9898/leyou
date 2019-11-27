package com.leyou.item.mapper;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import tk.mybatis.mapper.common.Mapper;

public interface SpecGroupMapper extends Mapper<SpecGroup> {
    void select(SpecParam record);
}
