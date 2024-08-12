package com.smart.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

//import com.smart.dao.UserRepository;
//import com.smart.entities.User;

@Controller
public class HomeController {

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	@Autowired
	private UserRepository userRepository;
	@GetMapping("/")
	public String Home(Model m) {
		m.addAttribute("title","Home- Smart Contact Manager");
		return "home";
	}
	
	@RequestMapping("/about")
	public String About(Model m) {
		m.addAttribute("title","About-Smart Contact Manager");
		return "about";
	}
	
	@RequestMapping("/signup")
	public String signup(Model m) {
		m.addAttribute("title","Register- Smart Contact Manager");
		m.addAttribute("user", new User());
		return "signup";
	}
	
	
	@RequestMapping(value="/do_register", method=RequestMethod.POST)
	public String registerUser(@Valid @ModelAttribute("user") User user,BindingResult result,
	                           @RequestParam(value="agreement", defaultValue="false") boolean agreement, 
	                           Model m, 
	                           HttpSession session) {
	    try {
	        if (!agreement) {
	            System.out.println("You have not agreed to the terms and conditions");
	            throw new Exception("You have not agreed to the terms and conditions");
	        }
	        if(result.hasErrors()) {
	        	System.out.println("ERROR "+result.toString());
	        	m.addAttribute("user", user);
	        	return "signup";
	        }
	        user.setRole("ROLE_USER");
	        user.setEnabled(true);
	        user.setImageUrl("banner.jpg");
	        user.setPassword(passwordEncoder.encode(user.getPassword()));
	        System.out.println(agreement);
	        System.out.println(user);
	        User u = this.userRepository.save(user);
	        session.setAttribute("message", new Message("Successfully registered!", "alert-success"));
	        m.addAttribute("user", new User());
	        return "signup";
	    } catch (Exception e) {
	        e.printStackTrace();
	        m.addAttribute("user", user);
	        session.setAttribute("message", new Message("Something went wrong: " + e.getMessage(), "alert-danger"));
	        return "signup";
	    }
	}
	//handler for custom login
	
	@GetMapping("/signin")
	public String customLogin(Model m) {
		m.addAttribute("title", "Smart Contact Manager");
		return "login";
	}

}
