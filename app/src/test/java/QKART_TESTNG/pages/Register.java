package QKART_TESTNG.pages;

import java.sql.Timestamp;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Register {
    RemoteWebDriver driver;
    String url = "https://crio-qkart-qa.vercel.app/register";
    public String lastGeneratedUsername = "";

    public Register(RemoteWebDriver driver) {
        this.driver = driver;
    }

    public void navigateToRegisterPage() {
        if (!driver.getCurrentUrl().equals(this.url)) {
            driver.get(this.url);
        }
    }

    public void clearTextbox(WebElement textBox) {
        new Actions(this.driver).click(textBox).keyDown(Keys.CONTROL).sendKeys("a")
                .keyUp(Keys.CONTROL).sendKeys(Keys.BACK_SPACE).perform();
    }
public Boolean registerUser(String Username, String Password, Boolean makeUsernameDynamic)
        throws InterruptedException {
    WebDriverWait wait = new WebDriverWait(driver, 10);

    // Wait and find the Username field
    WebElement username_txt_box = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")));

    // Generate dynamic or static username
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    String test_data_username = makeUsernameDynamic ? Username + "_" + timestamp.getTime() : Username;
    String test_data_password = Password;

    System.out.println("Registering user: " + test_data_username);

    clearTextbox(username_txt_box);
    username_txt_box.sendKeys(test_data_username);

    WebElement password_txt_box = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")));
    clearTextbox(password_txt_box);
    password_txt_box.sendKeys(test_data_password);

    WebElement confirm_password_txt_box = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("confirmPassword")));
    clearTextbox(confirm_password_txt_box);
    confirm_password_txt_box.sendKeys(test_data_password);

    WebElement register_now_button = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Register Now']")));
    register_now_button.click();

    try {
        wait.until(ExpectedConditions.urlToBe("https://crio-qkart-qa.vercel.app/login"));
    } catch (TimeoutException e) {
        System.out.println("Registration failed - login page not reached");
        return false;
    }

    this.lastGeneratedUsername = test_data_username;
    System.out.println("Registration successful. Redirected to login.");

    return driver.getCurrentUrl().endsWith("/login");
}

}
