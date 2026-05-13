package com.lsh.booking.mapper;

import com.lsh.booking.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author lsh
 * @since 2026-04-06
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
