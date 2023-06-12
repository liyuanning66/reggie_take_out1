package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());
        dishService.saveWithFlavor(dishDto);
        //在我们更新菜品列表的时候,清除菜品的数据
//        Set keys = redisTemplate.keys("dish_*");
        //上面的方法还是不好，不够精确,这样我们就能清理一部分缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return R.success("新增菜品成功");
    }

    /**
     * 分页查询展示
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        //构造分页构造器
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();
        //构造条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件,这里只有在name不为null的情况下
        queryWrapper.like(name != null, Dish::getName, name);
        //添加过排序条件，按getUpdateTime来排序
        queryWrapper.orderByDesc(Dish::getUpdateTime);
        //执行分页
        dishService.page(pageInfo, queryWrapper);
        //对象拷贝
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");
        //这里的getRecords()方法就是dish类的list集合
        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            Long id = item.getCategoryId();//查询id
            //通过id查询categoryName的名字
            Category category = categoryService.getById(id);
            if(category != null){
                String categoryName = category.getName();
                //然后在将名字赋给dishDto
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());
        dishDtoPage.setRecords(list);
        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     *http://localhost:8080/dish/1665358736929079298注意看这里的请求路径
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){//注意看上述操作的路径
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());
        dishService.updateWithFlavor(dishDto);
        //在我们更新菜品列表的时候,清除菜品的数据
//        Set keys = redisTemplate.keys("dish_*");
        //上面的方法还是不好，不够精确,这样我们就能清理一部分缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return R.success("修改菜品成功");
    }

    /**
     *
     * @return
     */
//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish){
//        //构造查询条件
//        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(Dish::getCategoryId, dish.getCategoryId());
//        //这里还要添加一个条件我们要加那种条件在售卖的
//        queryWrapper.eq(Dish::getStatus, 1);
//        //添加一个排序条件
//        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//        List<Dish> list = dishService.list(queryWrapper);
//        return R.success(list);
//    }

    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        /**
         * 优化代码
         */
        //动态的拼接key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        //获取缓存数据
        List<DishDto> dishDtoList = (List<DishDto>)redisTemplate.opsForValue().get(key);
        if(dishDtoList != null){
            return R.success(dishDtoList);
        }

        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null ,Dish::getCategoryId, dish.getCategoryId());
        //这里还要添加一个条件我们要加那种条件在售卖的
        queryWrapper.eq(Dish::getStatus, 1);
        //添加一个排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);
        dishDtoList = list.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            Long id = item.getCategoryId();//查询id
            //通过id查询categoryName的名字
            Category category = categoryService.getById(id);
            if(category != null){
                String categoryName = category.getName();
                //然后在将名字赋给dishDto
                dishDto.setCategoryName(categoryName);
            }
            //当前菜品的id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(DishFlavor::getDishId, dishId);
            List<DishFlavor>  dishFlavors = dishFlavorService.list(queryWrapper1);
            dishDto.setFlavors(dishFlavors);
            return dishDto;
        }).collect(Collectors.toList());
//        List<DishDto> dishDtoList = null;
        //如果不存在，记得缓存数据
        redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);
        return R.success(dishDtoList);
    }
}
