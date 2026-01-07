package QKART_TESTNG;

import QKART_TESTNG.pages.Checkout;
import QKART_TESTNG.pages.Home;
import QKART_TESTNG.pages.Login;
import QKART_TESTNG.pages.Register;
import QKART_TESTNG.pages.SearchResult;
import org.testng.annotations.Parameters;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import static org.testng.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.AfterSuite;
import org.testng.Assert;

public class QKART_Tests {

     RemoteWebDriver driver;
    public static String lastGeneratedUserName;

    @BeforeSuite(alwaysRun = true)
    public void createDriver() throws MalformedURLException {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setBrowserName(BrowserType.CHROME);
        driver = new RemoteWebDriver(new URL("http://localhost:8082/wd/hub"), capabilities);
        System.out.println("createDriver(): " + driver.getCapabilities().getBrowserName());
    }
    /*
     * Testcase01: Verify a new user can successfully register
     */
    @Test(priority = 1, description = "Verify registration happens correctly", groups = {"sanity"})
    @Parameters({"TC1_Username", "TC1_Password"})
    public void TestCase01(String username, String password) throws InterruptedException {
        Boolean status;

        // Visit the Registration page and register a new user
        Register registration = new Register(driver);
        registration.navigateToRegisterPage();
        status = registration.registerUser(username, password, true);
        Assert.assertTrue(status, "Failed to register new user");

        // Save the last generated username
        lastGeneratedUserName = registration.lastGeneratedUsername;

        // Visit the login page and login with the previously registered user
        Login login = new Login(driver);
        login.navigateToLoginPage();
        status = login.PerformLogin(lastGeneratedUserName, password);
        Assert.assertTrue(status, "Failed to login with registered user");

        // Visit the home page and log out the logged in user
        Home home = new Home(driver);
        status = home.PerformLogout();
        Assert.assertTrue(status, "Failed to logout");
    }

    /*
     * Testcase02: Verify that an existing user is not allowed to re-register on QKart
     */
    @Test(priority = 2, description = "Verify re-registering an already registered user fails", groups = {"sanity"})
    public void TestCase02() throws InterruptedException {
        Boolean status;

        // Visit the Registration page and register a new user
        Register registration = new Register(driver);
        registration.navigateToRegisterPage();
        status = registration.registerUser("testUser", "abc@123", true);
        Assert.assertTrue(status, "Failed to register new user");

        lastGeneratedUserName = registration.lastGeneratedUsername;

        registration.navigateToRegisterPage();
        status = registration.registerUser(lastGeneratedUserName, "abc@123", false);
        Assert.assertFalse(status, "User was able to re-register with existing username");
    }

    /*
     * Testcase03: Verify the functionality of the search text box
     */
    @Test(priority = 3, description = "Verify the functionality of search text box", groups = {"sanity"})
    public void TestCase03() throws InterruptedException {
        boolean status;

        // Visit the home page
        Home homePage = new Home(driver);
        homePage.navigateToHome();

        // Search for the "yonex" product
        status = homePage.searchForProduct("YONEX");
        Assert.assertTrue(status, "Unable to search for given product");

        // Fetch the search results
        List<WebElement> searchResults = homePage.getSearchResults();

        // Verify the search results are available
        Assert.assertTrue(searchResults.size() > 0, "There were no results for the given search string");

        for (WebElement webElement : searchResults) {
            // Create a SearchResult object from the parent element
            SearchResult resultelement = new SearchResult(webElement);

            // Verify that all results contain the searched text
            String elementText = resultelement.getTitleofResult();
            Assert.assertTrue(elementText.toUpperCase().contains("YONEX"), "Test Results contains un-expected values: " + elementText);
        }

        status = homePage.searchForProduct("Gesundheit");
        Assert.assertFalse(status, "Invalid keyword returned results");

        searchResults = homePage.getSearchResults();
        if (searchResults.size() == 0) {
            Assert.assertTrue(homePage.isNoResultFound(), "No products found message is not displayed");
        } else {
            Assert.fail("Expected: no results , actual: Results were available");
        }
    }

    /* Verify the presence of size chart and check if the size chart content is as expected */
    @Test(priority = 4, description = "Verify the existence of size chart for certain items and validate contents of size chart", groups = {"regression"})
    public void TestCase04() throws InterruptedException {
        boolean status = false;

        Home homePage = new Home(driver);
        homePage.navigateToHome();
        status = homePage.searchForProduct("Running Shoes");
        List<WebElement> searchResults = homePage.getSearchResults();

        List<String> expectedTableHeaders = Arrays.asList("Size", "UK/INDIA", "EU", "HEEL TO TOE");
        List<List<String>> expectedTableBody = Arrays.asList(Arrays.asList("6", "6", "40", "9.8"),
                Arrays.asList("7", "7", "41", "10.2"), Arrays.asList("8", "8", "42", "10.6"),
                Arrays.asList("9", "9", "43", "11"), Arrays.asList("10", "10", "44", "11.5"),
                Arrays.asList("11", "11", "45", "12.2"), Arrays.asList("12", "12", "46", "12.6"));

        for (WebElement webElement : searchResults) {
            SearchResult result = new SearchResult(webElement);

            if (result.verifySizeChartExists()) {
                status = result.verifyExistenceofSizeDropdown(driver);
                Assert.assertTrue(status, "Dropdown does not exist");

                if (result.openSizechart()) {
                    // Verify if the size chart contents matches the expected values
                    if (result.validateSizeChartContents(expectedTableHeaders, expectedTableBody, driver)) {
                        status = result.closeSizeChart(driver);
                        Assert.assertTrue(status, "Could not close size chart");
                    } else {
                        Assert.fail("Failed to validate contents of Size Chart Link");
                        status = false;
                    }
                } else {
                    Assert.fail("Failure to open Size Chart");
                    return;
                }
            } else {
                Assert.fail("Size Chart Link does not exist");
                return;
            }
        }
    }

    /*
     * Verify the complete flow of checking out and placing order for products is
     * working correctly
     */
    @Test(priority = 5, description = "Verify that a new user can add multiple products in to the cart and Checkout", groups = {"sanity"})
    @Parameters({"TC5_ProductNameToSearchFor", "TC5_ProductNameToSearchFor2", "TC5_AddressDetails"})
    public void TestCase05(String product1, String product2, String address) throws InterruptedException {
        Boolean status;

        // Go to the Register page
        Register registration = new Register(driver);
        registration.navigateToRegisterPage();
        status = registration.registerUser("testUser", "abc@123", true);
        Assert.assertTrue(status, "Happy Flow Test Failed during Registration");


        // Save the username of the newly registered user
        lastGeneratedUserName = registration.lastGeneratedUsername;

        // Go to the login page
        Login login = new Login(driver);
        login.navigateToLoginPage();
        status = login.PerformLogin(lastGeneratedUserName, "abc@123");
        Assert.assertTrue(status, "Happy Flow Test Failed during Login");

        // Go to the home page
        Home homePage = new Home(driver);
        homePage.navigateToHome();

        // Find required products by searching and add them to the user's cart
        status = homePage.searchForProduct(product1);
        homePage.addProductToCart(product1);
        status = homePage.searchForProduct(product2);
        homePage.addProductToCart(product2);
        homePage.clickCheckout();

        Checkout checkoutPage = new Checkout(driver);
        checkoutPage.addNewAddress(address);
        checkoutPage.selectAddress(address);
        checkoutPage.placeOrder();

        WebDriverWait wait = new WebDriverWait(driver, 30);
        wait.until(ExpectedConditions.urlToBe("https://crio-qkart-qa.vercel.app/thanks"));

        // Check if placing order redirected to the Thanks page
        status = driver.getCurrentUrl().endsWith("/thanks");
        Assert.assertTrue(status, "Failed to redirect to /thanks page");

        // Go to the home page
        homePage.navigateToHome();

        // Log out the user
        homePage.PerformLogout();
    }

    /*
     * Verify the quantity of items in cart can be updated
     */
    @Test(priority = 6, description = "Verify that the contents of the cart can be edited", groups = {"regression"})
    @Parameters({"TC6_ProductNameToSearch1", "TC6_ProductNameToSearch2"})
    public void TestCase06(String product1, String product2) throws InterruptedException {
        Boolean status;
        Home homePage = new Home(driver);
        Register registration = new Register(driver);
        Login login = new Login(driver);

        registration.navigateToRegisterPage();
        status = registration.registerUser("testUser", "abc@123", true);
        Assert.assertTrue(status, "User registration failed");

        lastGeneratedUserName = registration.lastGeneratedUsername;

        login.navigateToLoginPage();
        status = login.PerformLogin(lastGeneratedUserName, "abc@123");
        Assert.assertTrue(status, "User login failed");

        homePage.navigateToHome();
        status = homePage.searchForProduct(product1);
        homePage.addProductToCart(product1);

        status = homePage.searchForProduct(product2);
        homePage.addProductToCart(product2);
        homePage.changeProductQuantityinCart(product1, 2);
        homePage.changeProductQuantityinCart(product2, 0);
        homePage.changeProductQuantityinCart(product1, 1);
        homePage.clickCheckout();

        Checkout checkoutPage = new Checkout(driver);
        checkoutPage.addNewAddress("Addr line 1 addr Line 2 addr line 3");
        checkoutPage.selectAddress("Addr line 1 addr Line 2 addr line 3");

        checkoutPage.placeOrder();

        try {
            WebDriverWait wait = new WebDriverWait(driver, 30);
            wait.until(ExpectedConditions.urlToBe("https://crio-qkart-qa.vercel.app/thanks"));
        } catch (TimeoutException e) {
            Assert.fail("Error while placing order in: " + e.getMessage());
            return;
        }

        status = driver.getCurrentUrl().endsWith("/thanks");
        Assert.assertTrue(status, "Order placement was not successful");

        homePage.navigateToHome();
        homePage.PerformLogout();
    }

    @Test(priority = 7, description = "Verify that insufficient balance error is thrown when the wallet balance is not enough", groups = {"sanity"})
    @Parameters({"TC7_ProductName", "TC7_Qty"})
    public void TestCase07(String product, int quantity) throws InterruptedException {
        Boolean status;

        Register registration = new Register(driver);
        registration.navigateToRegisterPage();
        status = registration.registerUser("testUser", "abc@123", true);
        Assert.assertTrue(status, "User registration failed");

        lastGeneratedUserName = registration.lastGeneratedUsername;

        Login login = new Login(driver);
        login.navigateToLoginPage();
        status = login.PerformLogin(lastGeneratedUserName, "abc@123");
        Assert.assertTrue(status, "User login failed");

        Home homePage = new Home(driver);
        homePage.navigateToHome();
        status = homePage.searchForProduct(product);
        homePage.addProductToCart(product);

        homePage.changeProductQuantityinCart(product, quantity);

        homePage.clickCheckout();

        Checkout checkoutPage = new Checkout(driver);
        checkoutPage.addNewAddress("Addr line 1 addr Line 2 addr line 3");
        checkoutPage.selectAddress("Addr line 1 addr Line 2 addr line 3");

        checkoutPage.placeOrder();
        Thread.sleep(3000);

        status = checkoutPage.verifyInsufficientBalanceMessage();
        Assert.assertTrue(status, "Insufficient balance message not displayed");
    }

    @Test(priority = 8, description = "Verify that a product added to a cart is available when a new tab is added", groups = {"regression"})
    public void TestCase08() throws InterruptedException {
        Boolean status = false;

        Register registration = new Register(driver);
        registration.navigateToRegisterPage();
        status = registration.registerUser("testUser", "abc@123", true);
        Assert.assertTrue(status, "User registration failed");

        lastGeneratedUserName = registration.lastGeneratedUsername;

        Login login = new Login(driver);
        login.navigateToLoginPage();
        status = login.PerformLogin(lastGeneratedUserName, "abc@123");
        Assert.assertTrue(status, "User login failed");

        Home homePage = new Home(driver);
        homePage.navigateToHome();

        status = homePage.searchForProduct("YONEX");
        homePage.addProductToCart("YONEX Smash Badminton Racquet");

        String currentURL = driver.getCurrentUrl();

        driver.findElement(By.linkText("Privacy policy")).click();
        Set<String> handles = driver.getWindowHandles();
        driver.switchTo().window(handles.toArray(new String[handles.size()])[1]);

        driver.get(currentURL);
        Thread.sleep(2000);

        List<String> expectedResult = Arrays.asList("YONEX Smash Badminton Racquet");
        status = homePage.verifyCartContents(expectedResult);
        Assert.assertTrue(status, "Cart contents are not as expected");

        driver.close();

        driver.switchTo().window(handles.toArray(new String[handles.size()])[0]);
    }

    @Test(priority = 9, description = "Verify that privacy policy and about us links are working fine", groups = {"regression"})
    public void TestCase09() throws InterruptedException {
        Boolean status = false;

        Register registration = new Register(driver);
        registration.navigateToRegisterPage();
        status = registration.registerUser("testUser", "abc@123", true);
        Assert.assertTrue(status, "User registration failed");

        lastGeneratedUserName = registration.lastGeneratedUsername;

        Login login = new Login(driver);
        login.navigateToLoginPage();
        status = login.PerformLogin(lastGeneratedUserName, "abc@123");
        Assert.assertTrue(status, "User login failed");

        Home homePage = new Home(driver);
        homePage.navigateToHome();

        String basePageURL = driver.getCurrentUrl();

        driver.findElement(By.linkText("Privacy policy")).click();
        status = driver.getCurrentUrl().equals(basePageURL);
        Assert.assertTrue(status, "Verifying parent page url didn't change on privacy policy link click failed");

        Set<String> handles = driver.getWindowHandles();
        driver.switchTo().window(handles.toArray(new String[handles.size()])[1]);
        WebElement PrivacyPolicyHeading = driver.findElement(By.xpath("//*[@id=\"root\"]/div/div[2]/h2"));
        status = PrivacyPolicyHeading.getText().equals("Privacy Policy");
        Assert.assertTrue(status, "Verifying new tab opened has Privacy Policy page heading failed");

        driver.switchTo().window(handles.toArray(new String[handles.size()])[0]);
        driver.findElement(By.linkText("Terms of Service")).click();

        handles = driver.getWindowHandles();
        driver.switchTo().window(handles.toArray(new String[handles.size()])[2]);
        WebElement TOSHeading = driver.findElement(By.xpath("//*[@id=\"root\"]/div/div[2]/h2"));
        status = TOSHeading.getText().equals("Terms of Service");
        Assert.assertTrue(status, "Verifying new tab opened has Terms Of Service page heading failed");

        driver.close();
        driver.switchTo().window(handles.toArray(new String[handles.size()])[1]).close();
        driver.switchTo().window(handles.toArray(new String[handles.size()])[0]);
    }

    @Test(priority = 10, description = "Verify that the contact us dialog works fine", groups = {"regression"})
    public void TestCase10() throws InterruptedException {
        Home homePage = new Home(driver);
        homePage.navigateToHome();

        driver.findElement(By.xpath("//*[text()='Contact us']")).click();

        WebElement name = driver.findElement(By.xpath("//input[@placeholder='Name']"));
        name.sendKeys("crio user");
        WebElement email = driver.findElement(By.xpath("//input[@placeholder='Email']"));
        email.sendKeys("criouser@gmail.com");
        WebElement message = driver.findElement(By.xpath("//input[@placeholder='Message']"));
        message.sendKeys("Testing the contact us page");

        WebElement contactUs = driver.findElement(
                By.xpath("/html/body/div[2]/div[3]/div/section/div/div/div/form/div/div/div[4]/div/button"));

        contactUs.click();

        WebDriverWait wait = new WebDriverWait(driver, 30);
        wait.until(ExpectedConditions.invisibilityOf(contactUs));
        //If the code reaches here, the test has passed.
    }

    @Test(priority = 11, description = "Ensure that the Advertisement Links on the QKART page are clickable", groups = {"sanity"})
    public void TestCase11() throws InterruptedException {
        Boolean status = false;

        Register registration = new Register(driver);
        registration.navigateToRegisterPage();
        status = registration.registerUser("testUser", "abc@123", true);
        Assert.assertTrue(status, "User registration failed");

        lastGeneratedUserName = registration.lastGeneratedUsername;

        Login login = new Login(driver);
        login.navigateToLoginPage();
        status = login.PerformLogin(lastGeneratedUserName, "abc@123");
        Assert.assertTrue(status, "User login failed");

        Home homePage = new Home(driver);
        homePage.navigateToHome();

        status = homePage.searchForProduct("YONEX Smash Badminton Racquet");
        homePage.addProductToCart("YONEX Smash Badminton Racquet");
        homePage.changeProductQuantityinCart("YONEX Smash Badminton Racquet", 1);
        homePage.clickCheckout();

        Checkout checkoutPage = new Checkout(driver);
        checkoutPage.addNewAddress("Addr line 1  addr Line 2  addr line 3");
        checkoutPage.selectAddress("Addr line 1  addr Line 2  addr line 3");
        checkoutPage.placeOrder();
        Thread.sleep(3000);

        String currentURL = driver.getCurrentUrl();

        List<WebElement> Advertisements = driver.findElements(By.xpath("//iframe"));

        status = Advertisements.size() == 3;
        Assert.assertTrue(status, "Number of advertisements is not 3");

        WebElement Advertisement1 = driver.findElement(By.xpath("//*[@id=\"root\"]/div/div[2]/div/iframe[1]"));
        driver.switchTo().frame(Advertisement1);
        driver.findElement(By.xpath("//button[text()='Buy Now']")).click();
        driver.switchTo().parentFrame();

        status = !driver.getCurrentUrl().equals(currentURL);
        Assert.assertTrue(status, "Advertisement 1 is not clickable");

        driver.get(currentURL);
        Thread.sleep(3000);

        WebElement Advertisement2 = driver.findElement(By.xpath("//*[@id=\"root\"]/div/div[2]/div/iframe[2]"));
        driver.switchTo().frame(Advertisement2);
        driver.findElement(By.xpath("//button[text()='Buy Now']")).click();
        driver.switchTo().parentFrame();

        status = !driver.getCurrentUrl().equals(currentURL);
        Assert.assertTrue(status, "Advertisement 2 is not clickable");
    }

    @AfterSuite
    public void quitDriver() {
        System.out.println("quit()");
        driver.quit();
    }
}