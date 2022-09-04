package com.bookit.pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

public class HuntPage extends BasePage{

   @FindBy(id ="mat-input-0")
    public WebElement dateInput;

    @FindBy(xpath = "//span[.='from']")
    public WebElement fromDropdown;

    @FindBy(xpath = "//span[.='to']")
    public WebElement toDropdown;

    @FindBy(xpath = "//span[@class='mat-option-text']")
    public List<WebElement> timeList;

//    @FindBy(xpath = "//mat-option[@class='mat-option ng-star-inserted']")
//    public List <WebElement> timeToList;

      public String timeSlot(String time) {
       return "//span[.=' " + time + " ']";
      }


    @FindBy(xpath = "//mat-icon[.='search']")
    public WebElement searchBtn;


    @FindBy(xpath = "//p[@class='title is-size-4']")
    public List<WebElement> roomList;



}
