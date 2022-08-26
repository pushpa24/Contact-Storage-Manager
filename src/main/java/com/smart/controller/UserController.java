package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.persistence.criteria.Path;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

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
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		

		String userName= principal.getName();
		User user = userRepository.getUserByUserName(userName);
		model.addAttribute("user",user);
		
	}
	
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		
		model.addAttribute("title","User Dashboard");
		return "/normal/user_dashboard";
	}
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title","Add-Contact");
		model.addAttribute("contact",new Contact());
		return "normal/add_contact_form";
	}
	
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {
		
		try {
		
		String name=principal.getName();
		User user= this.userRepository.getUserByUserName(name);
		
		if(file.isEmpty())
		{
			contact.setImage("contact.png");
			
		}else {
			contact.setImage(file.getOriginalFilename());
			File saveFile=new ClassPathResource("static/img").getFile();
			java.nio.file.Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
			Files.copy(file.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);
			
		}
		
		
		user.getContacts().add(contact);
		contact.setUser(user);
		this.userRepository.save(user);
		session.setAttribute("message", new Message("Your contact is added!! Add more..","success"));
		
		}
		catch(Exception e) {
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went wrong","danger"));
		}
		return "normal/add_contact_form";
	}
	
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page")Integer page,Model m, Principal principal) {
		m.addAttribute("title","Show Contact");
		
		String userName= principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		Pageable pageable=PageRequest.of(page, 3);
		Page<Contact> contacts= this.contactRepository.findContactsByUser(user.getId(),pageable);
		
		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage",page);
		
		m.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show-contacts";
	}
	
	@RequestMapping("/{cid}/contact")
	public String showContactDetail(@PathVariable("cid") Integer cid, Model model,Principal principal) {
		
		Optional<Contact> contactOptional=this.contactRepository.findById(cid);
		Contact contact=contactOptional.get();
		
		String userName=principal.getName();
		User user=this.userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId())
		{
		model.addAttribute("contact",contact);
		}
		return "normal/contact_detail";
	}
	
	@GetMapping("/delete/{cid}")
	@Transactional
	public String deleteContact(@PathVariable("cid") Integer cid,Model model, HttpSession session,Principal principal) {
		//Optional<Contact> contactOptional=this.contactRepository.findById(cid);
		Contact contact=this.contactRepository.findById(cid).get();
		//contact.setUser(null);
		//this.contactRepository.delete(contact);
		User user=this.userRepository.getUserByUserName(principal.getName());
		user.getContacts().remove(contact);
		this.userRepository.save(user);
		session.setAttribute("message",new Message("Contact deleted successfully","success"));
		return "redirect:/user/show-contacts/0";
	}
	
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid ,Model m) {
		
		m.addAttribute("title","Update Form");
		Contact contact= this.contactRepository.findById(cid).get();
		m.addAttribute(contact);
		return "normal/update_form";
	}
	
	@RequestMapping(value="/process-update",method=RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,HttpSession session,Principal principal) {
		
		
		try {
			
			Contact contact2= this.contactRepository.findById(contact.getCid()).get();
			
			if(!file.isEmpty()) {
				File deleteFile=new ClassPathResource("static/img").getFile();
				File file1=new File(deleteFile,contact2.getImage());
				file1.delete();
				
				
				
				File saveFile=new ClassPathResource("static/img").getFile();
				java.nio.file.Path path=Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
				
				
				
			}else {
				contact.setImage(contact2.getImage());
			}
			User user=this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Your contact is updated","success"));
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return "redirect:/user/"+contact.getCid()+"/contact";
	}
	
	@GetMapping("/profile")
	public String yourProfile(Model m)
	{
		m.addAttribute("title","Profile Page");
		return "normal/profile";
		
	}
	
	@GetMapping("/settings")
	public String openSettings() {
		return "normal/settings";
	}
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,@RequestParam("newPassword") String newPassword,
			Principal principal, HttpSession session)
	{
		String UserName = principal.getName();
		User currentUser=this.userRepository.getUserByUserName(UserName);
		
		if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message", new Message("Your password is updated...","alert-success"));
		}
		else {
			session.setAttribute("message", new Message("Please enter correct old password","alert-danger"));
			return "redirect:/user/settings";
		}
		return "redirect:/user/index";
	}
	
	

}
