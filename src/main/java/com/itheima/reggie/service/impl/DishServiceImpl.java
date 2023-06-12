package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    DishFlavorService dishFlavorService;
    /**
     * 新增菜品
     * @param dishDto
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //保持菜品基本信息到菜品表
        this.save(dishDto);
        //这里我们需要重新获取菜品的id
        long id = dishDto.getId();
        //保存菜品数据到菜品口味表dish_flavor
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((item)-> {
            item.setDishId(id);
            return item;
        }).collect(Collectors.toList());
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @Override
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品基本信息，从dish表中查询, this就是当前这个类
        Dish dish = this.getById(id);
        //构造一个DishDto对象
        DishDto dishDto = new DishDto();
        //将dish中的数据全部拷贝到dishDto中
        BeanUtils.copyProperties(dish, dishDto);
        //查询当前菜品的口味信息，从dish_flavor表中查询
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(DishFlavor::getDishId, id);
        //得到结果
        List<DishFlavor> flavors = dishFlavorService.list(lambdaQueryWrapper);
        //设置到dishDto
        dishDto.setFlavors(flavors);
        return dishDto;
    }

    /**
     *更新菜品同时，更新菜品口味
     * @param dishDto
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        //更新菜品表基本信息
        this.updateById(dishDto);
        //更新口味表，因为口味表存储是一条一条的，所以我们的方案可以先清除当前口味，在加上新增
        //delete
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(queryWrapper);
        //insert
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((item)-> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());
        dishFlavorService.saveBatch(flavors);
    }
}
