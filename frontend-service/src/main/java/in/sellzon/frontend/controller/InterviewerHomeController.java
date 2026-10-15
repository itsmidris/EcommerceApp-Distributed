package in.sellzon.frontend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InterviewerHomeController {

    @GetMapping("/home")
    public String interviewerHome() {
        return "forward:/home.html";
    }
}
