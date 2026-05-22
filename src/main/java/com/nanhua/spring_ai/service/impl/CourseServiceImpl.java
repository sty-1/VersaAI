package com.nanhua.spring_ai.service.impl;

import com.nanhua.spring_ai.Entity.po.Course;
import com.nanhua.spring_ai.mapper.CourseMapper;
import com.nanhua.spring_ai.service.ICourseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 学科表 服务实现类
 * </p>
 *
 * @author yu
 * @since 2026-05-22
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {

}
