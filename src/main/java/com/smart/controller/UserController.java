package com.smart.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
//import com.smart.helper.Message;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ContactRepository contactRepository;
	
	//Method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model m,Principal principal) {
		String userName=principal.getName();
		System.out.println("Username:"+userName);
		User user=userRepository.getUserByUserName(userName);
		System.out.println("user"+user);
		m.addAttribute("user", user);
	}
	
	//dashboard home
	@RequestMapping("/index")
	public String dashboard(Model m,Principal principal) {
		m.addAttribute("title", "User");
		return "normal/user_dashboard";
	}
	//to open add contact form
	@GetMapping("/add_contact")
	public String openAddContactForm(Model m) {
		m.addAttribute("title", "Add Contact");
		m.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}
	
	//processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, 
	                             @RequestParam("imageFile") MultipartFile file, 
	                             Principal principal) {
	    try {
	        // Get the current user
	        System.out.println("DATA: " + contact);
	        String name = principal.getName();
	        User user = this.userRepository.getUserByUserName(name);

	        // Set the user to the contact
	        contact.setUser(user);

	        if (file.isEmpty()) {
	            // If no file is uploaded, set a default image
	            System.out.println("File not uploaded");
	            contact.setImage("contact.png");
	        } else {
	            // Handle the image upload
	            contact.setImage(file.getOriginalFilename());
	            File f = new ClassPathResource("/static/img").getFile();
	            Path path = Paths.get(f.getAbsolutePath() + File.separator + file.getOriginalFilename());
	            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
	        }

	        // Add the contact to the user's contacts list and save the user
	        user.getContacts().add(contact);
	        this.userRepository.save(user);

	        System.out.println("Added to database");

	        // Success message
	        return "normal/add_contact_form";

	    } catch (IOException e) {
	        // Error message
	        e.printStackTrace();
	    }

	    return "normal/add_contact_form";
	}

	//show contacts handler
	//per page=5[n]
	//current page=0[page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model m,Principal principal,HttpSession session) {
		m.addAttribute("title", "View");
		String userName=principal.getName();
		User user=this.userRepository.getUserByUserName(userName);
		Pageable pageable=PageRequest.of(page, 8);
		Page<Contact> contacts=this.contactRepository.findContactByUser(user.getId(),pageable);
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentpage", page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		if (session.getAttribute("message") != null) {
	        session.removeAttribute("message");
	    }
		return "normal/show_contacts";
	}
	
	//showing individual contact detail
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId,Model model,Principal principal) {
		System.out.println("CID"+cId);
		Optional<Contact> contactOptional=this.contactRepository.findById(cId);
		String userName=principal.getName();
		User user=this.userRepository.getUserByUserName(userName);
		Contact contact=contactOptional.get();
		if(user.getId()==contact.getUser().getId()) {
			model.addAttribute("title", contact.getName());
			model.addAttribute("contact", contact);
		}
		
		return "normal/contact_detail";
	}
	//Delete a user
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId,Model model,Principal principal,HttpSession session) {
		Optional<Contact> contactOptional=this.contactRepository.findById(cId);
		Contact contact=contactOptional.get();
		String userName=principal.getName();
		User user=this.userRepository.getUserByUserName(userName);
        Path path = Paths.get("/Users/shreeprabha/Documents/workspace-spring-tool-suite-4-4.23.1.RELEASE/smartcontactmanger/target/classes/static/img"+File.separator + contact.getImage());
        if (user.getId()==(contact.getUser().getId())) {
            try {
                // Delete the file if it exists
                if (Files.exists(path)) {
                    Files.delete(path);
                }
                
                // Delete the contact from the database
                this.contactRepository.delete(contact);
                session.setAttribute("message", new Message("Contact deleted successfully", "alert-success"));
            } catch (IOException e) {
                e.printStackTrace();
                session.setAttribute("message", new Message("Error deleting contact image", "alert-danger"));
            }
        } else {
            session.setAttribute("message", new Message("You are not authorized to delete this contact", "alert-danger"));
        }
		return "redirect:/user/show-contacts/0";
	}
	
	//open update form
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid,Model m) {
		m.addAttribute("title", "Update Contact");
		Contact contact=this.contactRepository.findById(cid).get();
		m.addAttribute("contact", contact);
		return "normal/update_form";
	}
	
	//update the contact handler
	@RequestMapping(value="/process-update", method=RequestMethod.POST)
	public String updateHandler(@RequestParam("cId") Integer cId, 
	                            @ModelAttribute Contact contact, 
	                            @RequestParam("imageFile") MultipartFile file, 
	                            Principal principal, 
	                            HttpSession session) {
	    try {
	        // Fetch the existing contact from the database
	        Optional<Contact> existingContactOptional = this.contactRepository.findById(cId);

	        if (!existingContactOptional.isPresent()) {
	            session.setAttribute("message", new Message("Contact not found", "alert-danger"));
	            return "redirect:/user/show-contacts/0"; // Redirect to a suitable page
	        }

	        Contact existingContact = existingContactOptional.get();
	        String name = principal.getName();
	        User user = this.userRepository.getUserByUserName(name);

	        // Update existing contact details
	        existingContact.setName(contact.getName());
	        existingContact.setSecondName(contact.getSecondName());
	        existingContact.setEmail(contact.getEmail());
	        existingContact.setPhone(contact.getPhone());
	        existingContact.setWork(contact.getWork());
	        existingContact.setDescription(contact.getDescription());

	        // Handle the image file if uploaded
	        if (!file.isEmpty()) {
	            // Delete the old image file if it exists
	            if (existingContact.getImage() != null) {
	                File oldFile = new ClassPathResource("/static/img").getFile();
	                Path oldPath = Paths.get(oldFile.getAbsolutePath() + File.separator + existingContact.getImage());
	                Files.deleteIfExists(oldPath);
	            }

	            // Save the new file to the static/img directory
	            existingContact.setImage(file.getOriginalFilename());
	            File f = new ClassPathResource("/static/img").getFile();
	            Path path = Paths.get(f.getAbsolutePath() + File.separator + file.getOriginalFilename());
	            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
	        }

	        // Set the user to the contact
	        existingContact.setUser(user);

	        // Save the updated contact
	        this.contactRepository.save(existingContact);

	        // Success message
	        session.setAttribute("message", new Message("Contact updated successfully", "alert-success"));
	        return "redirect:/user/show-contacts/0";

	    } catch (IOException e) {
	        e.printStackTrace();
	        session.setAttribute("message", new Message("Something went wrong", "alert-danger"));
	        return "redirect:/user/show-contacts/0"; // Redirect to a suitable page in case of error
	    }
	}
	   @GetMapping("/profile")
		public String yourProfile(Model m) {
		   m.addAttribute("title","Profile");
			return "normal/profile";
		}
	 //open settings handler
	   @GetMapping("/settings")
	   public String openSettings() {
		   return "normal/settings";
	   }
	   
	   //change password 
	   @PostMapping("/change-password")
	   public String changePassword(@RequestParam("oldPassword") String oldPassword,@RequestParam("newPassword") String newPassword,Principal principal, HttpSession session) {
		   System.out.println("OLD PASSWORD "+oldPassword);
		   System.out.println("NEW PASSWORD "+newPassword);
		   String userName=principal.getName();
		   User currentUser=this.userRepository.getUserByUserName(userName);
		   System.out.println(currentUser.getPassword());
		   if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			   //change the password
			   currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			   this.userRepository.save(currentUser);
			   session.setAttribute("message", new Message("Your password is successfully changed","alert-success"));
		   }else {
			   //error
			   session.setAttribute("message", new Message("Incorrect current password","alert-danger"));
		   }
		   return "redirect:/user/index";
	   }
}

