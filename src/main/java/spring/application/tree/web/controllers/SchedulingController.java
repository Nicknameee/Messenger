package spring.application.tree.web.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.application.tree.data.scheduling.service.ScheduleService;

@RestController
@RequestMapping("/api/schedule")
@Slf4j
@RequiredArgsConstructor
public class SchedulingController {
    private final ScheduleService scheduleService;
}
