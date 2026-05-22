package com.nanhua.spring_ai.tools;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.nanhua.spring_ai.Entity.po.Course;
import com.nanhua.spring_ai.Entity.po.CourseReservation;
import com.nanhua.spring_ai.Entity.po.School;
import com.nanhua.spring_ai.Entity.query.CourseQuery;
import com.nanhua.spring_ai.repository.ChatHistoryRepository;
import com.nanhua.spring_ai.service.ICourseReservationService;
import com.nanhua.spring_ai.service.ICourseService;
import com.nanhua.spring_ai.service.ISchoolService;
import com.nanhua.spring_ai.service.impl.SchoolServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CourseTools {
    private final ICourseService courseService;
    private final ISchoolService schoolService;
    private final ICourseReservationService courseReservationService;
    @Tool(description="根据条件查询课程")
    public List<Course> queryCourse(@ToolParam( description = "课程查询条件") CourseQuery  query){
        if(query==null)
        {
            return courseService.list();
        }
        QueryChainWrapper<Course> wrapper = courseService.query()
                .eq(query.getType() != null, "type", query.getType())
                .le(query.getEdu() != null, "edu", query.getEdu());

        if(query.getSorts()!=null&&query.getSorts().size()>0)
        {
            for(CourseQuery.Sort sort:query.getSorts())
            {
                wrapper.orderBy(true,sort.getAsc(), sort.getField());
            }
        }
        return wrapper.list();
    }

    @Tool(description="查询校区")
    public List<School> querySchool(){
        return schoolService.list();
    }

    @Tool(description="生成预约单，返回预约单号")
    public Integer queryCourseReservation(
            @ToolParam(description = "课程名称") String courseName,
            @ToolParam(description = "学生姓名")String studentName,
            @ToolParam(description = "联系方式")String contactInfo,
            @ToolParam(description = "预约校区")String school,
            @ToolParam(description = "备注")String remark
    ){
        CourseReservation courseReservation = new CourseReservation();
        courseReservation.setCourse(courseName);
        courseReservation.setStudentName(studentName);
        courseReservation.setContactInfo(contactInfo);
        courseReservation.setSchool(school);
        courseReservation.setRemark(remark);
        courseReservationService.save(courseReservation);
        return courseReservation.getId();
    }

}
