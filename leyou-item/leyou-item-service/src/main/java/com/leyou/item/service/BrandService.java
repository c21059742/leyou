package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;

    /**
     * 根据查询条件分页并排序查询品牌信息
     * @param key
     * @param page
     * @param rows
     * @param sortBy
     * @param desc
     * @return
     */
    public PageResult<Brand> queryBrandsByPage(String key, Integer page, Integer rows, String sortBy, Boolean desc) {
        //初始化Example对象
        Example example = new Example(Brand.class);
        //拼接的条件对象
        Example.Criteria criteria = example.createCriteria();

        //key：根据name进行模糊查询，或者根据首字母进行查询
        if (StringUtils.isNotBlank(key)){
            //letter是首字母
            criteria.andLike("name","%"+key+"%").orEqualTo("letter",key);
        }

        //添加分页条件
        PageHelper.startPage(page,rows);

        //添加排序条件
        if (StringUtils.isNotBlank(sortBy)){
            example.setOrderByClause(sortBy+" "+(desc?"desc":"asc"));
        }

        List<Brand> brands = this.brandMapper.selectByExample(example);

        //把查询结果包装成pageInfo对象
        PageInfo<Brand> pageInfo = new PageInfo<>(brands);
        //包装成分页结果集进行返回
        return new PageResult<>(pageInfo.getTotal(),pageInfo.getList());
    }

    /**
     * 新增品牌
     * @param brand
     * @param cids
     */
    @Transactional
    public void saveBrand(Brand brand, List<Long> cids) {

        //向brand表中保存数据
        //如果以下结果为true，说明保存成功，
        Boolean flag = this.brandMapper.insertSelective(brand) == 1;

        //如果flag为true再向中间表保存数据
        //同用mapper支支持单表操作
        if (flag){
            cids.forEach(cid->{
                this.brandMapper.insertCategoryAndBrand(cid,brand.getId());
            });
        }
    }


    @Transactional
    public void changeBrand(Brand brand, List<Long> cids) {
        Long bid = brand.getId();
        brandMapper.deleteCategoryBrand(bid);

        brandMapper.updateByPrimaryKeySelective(brand);

        cids.forEach((cid)->{
            brandMapper.insertCategoryAndBrand(cid,bid);
        });
    }

    public void deleteBrand(Long bid) {
        brandMapper.deleteByPrimaryKey(bid);
        brandMapper.deleteCategoryBrand(bid);

    }

    /**
     * 2020-2-25
     * 根据分类id查询品牌
     * @param cid
     * @return
     */
    public List<Brand> queryBrandByCid(Long cid) {
        return this.brandMapper.selectBrandByCid(cid);
    }




    /*public List<Brand> queryBrandsByCid(Long cid) {
        return this.brandMapper.selectBrandsByCid(cid);
    }*/

    public Brand queryBrandsById(Long id) {
        return this.brandMapper.selectByPrimaryKey(id);

    }
}
