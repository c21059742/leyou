package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
    @Autowired
    private BrandClient brandClient;
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;
    @Autowired
    private GoodsRepository goodsRepository;


    //skus是String类型 将skus集合转为json工具：fastJson<jackson<gson
    private static final ObjectMapper MAPPER = new ObjectMapper();


    //声明一个转变为goods方法
    public Goods buildGoods(Spu spu) throws IOException {
        Goods goods = new Goods();

        //根据spu中的品牌id查询品牌
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());
        List<String> names = this.categoryClient.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

        //根据spuId查询skus
        List<Sku> skus = this.goodsClient.querySkusBySpuId(spu.getId());
        //价格也是一个集合
        //定义集合用来保存价格
        ArrayList<Long> prices = new ArrayList<>();

        //sku中并不需要所有的东西，只需要id title image price
        //初始化一个skuMapList，每一个map相当于一个sku，map中key值:id title image price
        List<Map<String,Object>> skuMapLiat  = new ArrayList<>();
        skus.forEach(sku -> {
            prices.add(sku.getPrice());
            Map<String, Object> skuMap = new HashMap<>();
            skuMap.put("id",sku.getId());
            skuMap.put("title",sku.getTitle());
            skuMap.put("images",StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(),",")[0]);
            skuMap.put("price",sku.getPrice());
            skuMapLiat.add(skuMap);
        });

        //查询spuDetail，目的是为了拿到两个字段genericSpec,SpecialSpec
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spu.getId());
        //将数据库中json反序列化为map
        Map<Long,Object> genericSpecMap = MAPPER.readValue(spuDetail.getGenericSpec(), new TypeReference<Map<Long, Object>>(){});
        Map<Long, List<Object>> specialSpecMap = MAPPER.readValue(spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<Object>>>(){});

        //查询所有可以搜索的规格参数
        List<SpecParam> params = this.specificationClient.queryParams(null, spu.getCid3(), null, true);
        HashMap<String, Object> specs = new HashMap<>();
        params.forEach(param->{
            //判断规格参数是否通用
            if (param.getGeneric() && genericSpecMap.get(param.getId()) != null){
                String value = genericSpecMap.get(param.getId()).toString();
                //判断是不是数值类型
                if (param.getNumerical()){
                    //如果参数是数值的话，要返回范围
                    value = chooseSegment(value,param);
                }
                specs.put(param.getName(),value);
            }else {
                //特殊的规格参数
                List<Object> value = specialSpecMap.get(param.getId());
                specs.put(param.getName(),value);
            }
        });

        //把spu中简单的一些字段赋值给goods对象
        BeanUtils.copyProperties(spu,goods);
        //中间隔开 以防分词出现问题
        //标题 分类 品牌
        goods.setAll(spu.getSubTitle()+""+brand.getName()+""+ StringUtils.join(names,""));
        goods.setPrice(prices);
        goods.setSkus(MAPPER.writeValueAsString(skuMapLiat));//将输入的内容转为字符串
        goods.setSpecs(specs);
        return goods;
    }

    private String chooseSegment(String value,SpecParam p){
        //分隔数字1.5-2.5
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        //保存数值段
        for (String segment : p.getSegments().split(",")){
            String[] segs = segment.split("-");
            //获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            //判断是否在范围内
            if (val >=begin && val < end){
                if (segs.length == 1){
                    result = segs[0]+p.getUnit()+"以上";
                }else if (begin == 0){
                    result = segs[1]+p.getUnit()+"以下";
                }else {
                    result = segment+p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    /**
     * 用户基本查询
     * 用户搜索
     * @param searchRequest
     * @return
     */

    public SearchResult search(SearchRequest searchRequest) {
        if (StringUtils.isBlank(searchRequest.getKey())){
            return null;
        }
        //自定义查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //添加查询条件
        //MatchQueryBuilder basicQuery = QueryBuilders.matchQuery("all", searchRequest.getKey()).operator(Operator.AND);
        BoolQueryBuilder basicQuery = buildBasicQuery(searchRequest);
        queryBuilder.withQuery(basicQuery);
        //添加分页 去es分页  分页是从0开始 为2的时候是第一页
        queryBuilder.withPageable(PageRequest.of(searchRequest.getPage()-1,searchRequest.getSize()));
        //添加结果集过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","skus","subTitle"},null));

        //添加分类和品牌的聚合(3.12)
        String brandAggName = "brands";
        String  categoryAggName = "categories";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));

        //执行查询，获取结果集
//        Page<Goods> goodsPage = this.goodsRepository.search(queryBuilder.build());
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>)this.goodsRepository.search(queryBuilder.build());

        //定义方法解析聚合结果集
        List<Brand> brands = getBrandAggResult(goodsPage.getAggregation(brandAggName));
        List<Map<String,Object>> categories = getCategoryAggResult(goodsPage.getAggregation(categoryAggName));

        List<Map<String,Object>> specs = null;
        //分类集合中只有一个分类的时候，才会进行规格参数的聚合
        if (!CollectionUtils.isEmpty(categories) && categories.size() == 1){
            specs = getParamAggName((Long) categories.get(0).get("id"),basicQuery);
        }
        //返回的查询对象
        return new SearchResult(goodsPage.getTotalElements(),goodsPage.getTotalPages(),goodsPage.getContent(),categories,brands,specs);
    }

    /**
     * 构建boolQueryBuild
     * @param searchRequest
     * @return
     */
    private BoolQueryBuilder buildBasicQuery(SearchRequest searchRequest) {
        //初始化bool查询器
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //添加基本的查询条件，
        boolQueryBuilder.must(QueryBuilders.matchQuery("all",searchRequest.getKey()).operator(Operator.AND));
        //添加过滤
        for (Map.Entry<String, String> entry : searchRequest.getFilter().entrySet()) {
            //判断用户输入的查询条件是品牌，分类，还是规格参数
            String key = entry.getKey();
            if (StringUtils.equals(key,"品牌")){
                key = "brandId";
            }else if (StringUtils.equals(key,"分类")){
                key = "cid3";
            }else {
                key="specs."+key+".keyword";
            }
            boolQueryBuilder.filter(QueryBuilders.termQuery(key,entry.getValue()));
        }
        return boolQueryBuilder;
    }

    /**
     * 3-13
     * 规格参数的聚合
     * @param id
     * @param basicQuery
     * @return
     */
    private List<Map<String, Object>> getParamAggName(Long id, QueryBuilder basicQuery) {
        //查询出可以聚合的规格参数
        List<SpecParam> params = this.specificationClient.queryParams(null, id, null, true);
        //自定义查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(basicQuery);
        //添加规格参数的聚合
        params.forEach(param->{
            //keyword表示不分词
            queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("specs."+param.getName()+".keyword"));
        });
        //添加结果集过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{},null));//size:0
        //执行查询
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>)this.goodsRepository.search(queryBuilder.build());
        //初始化聚合结果集
        List<Map<String,Object>> paramMapList = new ArrayList<>();
        //获取所有规格参数的结果集map<paramName,聚合结果集>
        Map<String, Aggregation> stringAggregationMap = goodsPage.getAggregations().asMap();
        //遍历规格参数聚合结果集，对应一个map
        for (Map.Entry<String, Aggregation> entry : stringAggregationMap.entrySet()) {
            //每一个规格参数的聚合结果集
            HashMap<String, Object> map = new HashMap<>();
            //设置k字段
            map.put("k",entry.getKey());
            //这里边放的是所有的key的值
//            ArrayList<Object> options = new ArrayList<>();

            //解析每一个聚合中的桶
            StringTerms terms = (StringTerms)entry.getValue();
            //获取桶
            List<Object> options = terms.getBuckets().stream().map(bucket -> bucket.getKeyAsString()).collect(Collectors.toList());
            //设置options字段
            map.put("options",options);
            paramMapList.add(map);
        }
        return paramMapList;

    }


    /**
     * 解析分类的聚合结果集
     * @param aggregation
     * @return
     */
    private List<Map<String,Object>> getCategoryAggResult(Aggregation aggregation) {
        //请转成LongTerms
        LongTerms terms = (LongTerms) aggregation;
        //获取桶的集合
        return terms.getBuckets().stream().map(bucket -> {
            Map<String,Object> map = new HashMap<>();
            //获取到分类id
            long id = bucket.getKeyAsNumber().longValue();
            //获取分类名称
            List<String> names = this.categoryClient.queryNamesByIds(Arrays.asList(id));
            map.put("id",id);
            map.put("name",names.get(0));
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 解析品牌的聚合结果集
     * @param aggregation
     * @return
     */
    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        //请转成LongTerms
        LongTerms terms = (LongTerms) aggregation;
        //获取桶的集合
        return terms.getBuckets().stream().map(bucket -> {
            //品牌id
            long brandId = bucket.getKeyAsNumber().longValue();
            //通过id查询品牌对象
            return this.brandClient.queryBrandById(brandId);
        }).collect(Collectors.toList());
    }

    /**
     * 更新索引库
     * @param spuId
     */
    public void saveIndex(Long spuId) throws IOException {
        Spu spu = this.goodsClient.querySpuById(spuId);
        Goods goods = this.buildGoods(spu);
        this.goodsRepository.save(goods);
    }

    /**
     * 根据spuId删除索引
     * @param spuId
     */
    public void deleteIndex(Long spuId) {
        this.goodsRepository.deleteById(spuId);
    }
}
