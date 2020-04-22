package com.leyou.item.service;

import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class SpecificationService {
    @Autowired
    private SpecGroupMapper specGroupMapper;
    @Autowired
    private SpecParamMapper specParamMapper;
    /**
     * 根据分类id查询规格参数
     * @param cid
     * @return
     */
    public List<SpecGroup> queryGroupByCid(Long cid) {
        SpecGroup specGroup = new SpecGroup();
        specGroup.setCid(cid);
        return this.specGroupMapper.select(specGroup);
    }

    /**
     * 根据分组id查询规格参数
     * @param gid
     * @return
     */
    public List<SpecParam> queryParam(Long gid,Long cid,Boolean generic,Boolean searching) {
        SpecParam specParam = new SpecParam();
        specParam.setGroupId(gid);
        specParam.setCid(cid);
        specParam.setGeneric(generic);
        specParam.setSearching(searching);
        return this.specParamMapper.select(specParam);
    }

    public List<SpecGroup> queryGroupWithParam(Long cid) {
        //查询组
        List<SpecGroup> groups = this.queryGroupByCid(cid);
        groups.forEach(group -> {
            //查询组对应的规格参数
            List<SpecParam> params = this.queryParam(group.getId(), null, null, null);
            group.setParams(params);
        });
        return groups;

    }
}
