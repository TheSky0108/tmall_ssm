package com.li.tmall.controller;

import com.github.pagehelper.PageHelper;
import com.li.tmall.pojo.*;
import com.li.tmall.service.*;
import com.sun.org.apache.xpath.internal.operations.Mod;
import comparator.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author: 98050
 * Time: 2018-09-27 09:24
 * Feature:
 */
@Controller
@RequestMapping("")
public class ForeController {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ProductService productService;
    @Autowired
    private UserService userService;
    @Autowired
    private ProductImageService productImageService;
    @Autowired
    private PropertyValueService propertyValueService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private ReviewService reviewService;

    @RequestMapping("forehome")
    public String home(Model model) {
        List<Category> cs= categoryService.list();
        productService.fill(cs);
        productService.fillByRow(cs);
        model.addAttribute("cs", cs);
        return "fore/home";
    }

    @RequestMapping("foreregister")
    public String register(Model model, User user){
        String name = user.getName();
        //把账号里的特殊符号进行转义
        name = HtmlUtils.htmlEscape(name);
        user.setName(name);
        boolean exist = userService.isExist(name);
        if (exist){
            String message = "用户名已经被使用,不能使用!";
            model.addAttribute("msg",message);
            model.addAttribute("user",null);
            return "fore/register";
        }
        userService.add(user);
        return "redirect:registerSuccess";
    }

    @RequestMapping("registerSuccess")
    public String registerSuccess(){
        return "fore/registerSuccess";
    }

    @RequestMapping("forelogin")
    public String login(@RequestParam("name") String name, @RequestParam("password") String password, Model model, HttpSession session){
        name = HtmlUtils.htmlEscape(name);
        User user = userService.get(name,password);

        if (user == null){
            model.addAttribute("msg","账号密码错误！");
            return "fore/login";
        }
        session.setAttribute("user",user);
        return "redirect:forehome";
    }

    @RequestMapping("forelogout")
    public String logout(HttpSession session){
        session.removeAttribute("user");
        return "redirect:forehome";
    }

    @RequestMapping("foreproduct")
    public String product(int pid,Model model){
        Product p = productService.get(pid);

        List<ProductImage> productSingleImages = productImageService.list(p.getId(), ProductImageService.TYPE_SINGLE);
        List<ProductImage> productDetailImages = productImageService.list(p.getId(), ProductImageService.TYPE_DETAIL);
        p.setProductSingleImages(productSingleImages);
        p.setProductDetailImages(productDetailImages);

        List<PropertyValue> pvs = propertyValueService.list(p.getId());
        List<Review> reviews = reviewService.list(p.getId());
        productService.setSaleAndReviewNumber(p);
        model.addAttribute("reviews", reviews);
        model.addAttribute("p", p);
        model.addAttribute("pvs", pvs);
        return "fore/product";
    }

    @RequestMapping("forecheckLogin")
    @ResponseBody
    public String checkLogin(HttpSession session){
        User user = (User) session.getAttribute("user");
        if (user != null){
            return "success";
        }
        return "fail";
    }

    @RequestMapping("foreloginAjax")
    @ResponseBody
    public String loginAjax(@RequestParam("name") String name,@RequestParam("password") String password,HttpSession session){
        name = HtmlUtils.htmlEscape(name);
        User user = userService.get(name,password);

        if(null == user){
            return "fail";
        }
        session.setAttribute("user", user);
        return "success";
    }

    @RequestMapping("forecategory")
    public String category(@RequestParam("cid") int cid,String sort, Model model) {
        Category c = categoryService.get(cid);
        productService.fill(c);
        productService.setSaleAndReviewNumber(c.getProducts());

        if(null!=sort){
            switch(sort){
                case "review":
                    Collections.sort(c.getProducts(),new ProductReviewComparator());
                    break;
                case "date" :
                    Collections.sort(c.getProducts(),new ProductDateComparator());
                    break;

                case "saleCount" :
                    Collections.sort(c.getProducts(),new ProductSaleCountComparator());
                    break;

                case "price":
                    Collections.sort(c.getProducts(),new ProductPriceComparator());
                    break;

                case "all":
                    Collections.sort(c.getProducts(),new ProductAllComparator());
                    break;

                default:
            }
        }

        model.addAttribute("c", c);
        return "fore/category";
    }

    @RequestMapping("foresearch")
    public String search( @RequestParam("keyword") String keyword,String sort, Model model){

        PageHelper.offsetPage(0,20);
        List<Product> products = productService.search(keyword);
        productService.setSaleAndReviewNumber(products);

        if(null!=sort){
            switch(sort){
                case "review":
                    Collections.sort(products,new ProductReviewComparator());
                    break;
                case "date" :
                    Collections.sort(products,new ProductDateComparator());
                    break;

                case "saleCount" :
                    Collections.sort(products,new ProductSaleCountComparator());
                    break;

                case "price":
                    Collections.sort(products,new ProductPriceComparator());
                    break;

                case "all":
                    Collections.sort(products,new ProductAllComparator());
                    break;

                default:
            }
        }

        model.addAttribute("ps",products);
        model.addAttribute("keyword",keyword);
        return "fore/searchResult";
    }

    @RequestMapping("forebuyone")
    public String buyone(@RequestParam("pid") int pid,@RequestParam("num") int num,HttpSession session){
        Product product =productService.get(pid);
        User user = (User) session.getAttribute("user");

        boolean found = false;
        int orderItemId = 0;

        List<OrderItem> orderItems = orderItemService.listByUser(user.getId());
        for (OrderItem orderItem : orderItems){
            if (orderItem.getPid().intValue() == product.getId().intValue()){
                orderItem.setNumber(orderItem.getNumber()+num);
                orderItemService.update(orderItem);
                found = true;
                orderItemId = orderItem.getId();
                break;
            }
        }

        if (!found){
            OrderItem orderItem = new OrderItem();
            orderItem.setUid(user.getId());
            orderItem.setNumber(num);
            orderItem.setPid(product.getId());
            orderItemService.add(orderItem);
            //mybatis自动生成id
            System.out.println(orderItem.getId());
            orderItemId = orderItem.getId();
        }
        return "redirect:forebuy?oiid="+orderItemId;
    }

    @RequestMapping("forebuy")
    public String buy(Model model,String[] oiid,HttpSession session){
        List<OrderItem> orderItems = new ArrayList<>();
        float total = 0;
        for (String str_id : oiid){
            int id = Integer.parseInt(str_id);
            OrderItem orderItem = orderItemService.get(id);
            total += orderItem.getNumber()*orderItem.getProduct().getPromotePrice();
            orderItems.add(orderItem);
        }

        session.setAttribute("ois",orderItems);
        model.addAttribute("total",total);
        return "fore/buy";
    }

    @RequestMapping("foreaddCart")
    @ResponseBody
    public String addCart(@RequestParam("pid") int pid,@RequestParam("num") int num,HttpSession session){
        Product product =productService.get(pid);
        User user = (User) session.getAttribute("user");

        boolean found = false;
        List<OrderItem> orderItems = orderItemService.listByUser(user.getId());
        for (OrderItem orderItem : orderItems){
            if (orderItem.getPid().intValue() == product.getId().intValue()){
                orderItem.setNumber(orderItem.getNumber()+num);
                orderItemService.update(orderItem);
                found = true;
                break;
            }
        }

        if (!found){
            OrderItem orderItem = new OrderItem();
            orderItem.setUid(user.getId());
            orderItem.setNumber(num);
            orderItem.setPid(product.getId());
            orderItemService.add(orderItem);
        }
        return "success";
    }

    @RequestMapping("forecart")
    public String cart(Model model,HttpSession session){
        User user = (User) session.getAttribute("user");
        List<OrderItem> orderItems = orderItemService.listByUser(user.getId());
        model.addAttribute("ois",orderItems);
        return "fore/cart";
    }

    @RequestMapping("forechangeOrderItem")
    @ResponseBody
    public String changeOrderItem(@RequestParam("pid") int pid,@RequestParam("number") int number,Model model,HttpSession session){
        User user = (User) session.getAttribute("user");
        if (user == null){
            return "fail";
        }else {
            List<OrderItem> orderItems = orderItemService.listByUser(user.getId());
            for (OrderItem orderItem : orderItems){
                if (orderItem.getPid() == pid){
                    orderItem.setNumber(number);
                    orderItemService.update(orderItem);
                    break;
                }
            }
            return "success";
        }
    }

    @RequestMapping("foredeleteOrderItem")
    @ResponseBody
    public String deleteOrderItem(@RequestParam("oiid") int oiid,Model model,HttpSession session){
        User user = (User)session.getAttribute("user");
        if (user != null){
            orderItemService.delete(oiid);
            return "success";
        }else {
            return "fail";
        }
    }


}
