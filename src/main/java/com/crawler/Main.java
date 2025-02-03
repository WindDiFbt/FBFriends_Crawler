package com.crawler;

import com.google.gson.Gson;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws WebDriverException, InterruptedException {
        System.setProperty("webdriver.chrome.driver", "D:\\Documents\\Driver\\chromedriver-win64\\chromedriver.exe");
        // Set up Chrome options to configure the browser behavior
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new"); // Open Chrome in hidden mode
        options.addArguments("--disable-blink-features=AutomationControlled"); // Disable automation flag in Chrome
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"}); // Disable automation-related switches
        options.addArguments("--disable-notifications"); // Disable notifications
        options.addArguments("--incognito"); // Open in incognito mode
        options.addArguments("--disable-gpu"); // Disable GPU hardware acceleration
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.managed_default_content_settings.images", 2); // Disable images
        prefs.put("profile.default_content_setting_values.notifications", 2); // Disable notifications in incognito
        options.setExperimentalOption("prefs", prefs); // Apply preferences
        // Create a new ChromeDriver instance and start the browser
        ChromeDriver browser = new ChromeDriver(options);
        // Perform login to Facebook
        login(browser);
        Thread.sleep(3000);
        Scanner sc = new Scanner(System.in);
        String choice;
        // Loop to allow user input for crawling different user's friends
        do {
            System.out.print("Enter UID of target user: ");
            String id = sc.nextLine();
            String profileUrl = "https://www.facebook.com/" + id;
            System.out.println("The program is running, please wait!");
            // Navigate to the target user's friends page
            browser.get(profileUrl + "/friends");
            Set<User> friendSet = crawlFriendsOfUser(browser, profileUrl);
            if (friendSet == null) {
                System.out.println("UID not found!");
            } else if (friendSet.isEmpty()) {
                System.out.println("No friends found");
            } else {
                Gson gson = new Gson();
                String json = gson.toJson(friendSet);
                System.out.println(json);
                System.out.println("Total " + friendSet.size() + " users found!");
            }
            System.out.print("Do you want to continue program? (Y/N): ");
            choice = sc.nextLine();
        } while (choice.equalsIgnoreCase("Y"));
        browser.quit();
    }

    /**
     * Method to perform login on Facebook.
     * This method automates the login process by entering the user's email and password.
     * It simulates typing each character with random delays to avoid detection.
     */
    private static void login(WebDriver browser) throws InterruptedException {
        Random rand = new Random();
        // Open Facebook login page
        browser.get("https://www.facebook.com");
        Thread.sleep(rand.nextInt(1000, 5000));
        // Enter the email in the email field
        WebElement email = browser.findElement(By.id("email"));
        for (char c : "###".toCharArray()) {
            email.sendKeys(String.valueOf(c));
            Thread.sleep(rand.nextInt(100, 300));
        }
        // Enter the password in the password field
        WebElement pass = browser.findElement(By.id("pass"));
        for (char c : "###".toCharArray()) {
            pass.sendKeys(String.valueOf(c));
            Thread.sleep(rand.nextInt(100, 300));
        }
        // Wait for some time and then press Enter to log in
        Thread.sleep(rand.nextInt(2000, 5000));
        browser.findElement(By.name("login")).sendKeys(Keys.ENTER);
    }

    /**
     * Method to crawl the friends of a target user.
     * This method continuously scrolls through the user's friends page, collecting their data.
     * It stops once all friends are loaded or after 60 seconds.
     *
     * @param browser    The WebDriver instance to interact with the browser.
     * @param profileUrl The URL of the target user's profile.
     * @return A set of unique User objects representing the friends of the target user.
     */
    private static Set<User> crawlFriendsOfUser(WebDriver browser, String profileUrl) throws InterruptedException {
        // Wait for the page to load
        Thread.sleep(5000);
        int lastHeight = 0;
        long startTime = System.currentTimeMillis();
        // Crawl for a maximum of 60 seconds
        long maxDuration = 60 * 1000;
        Set<User> friendSet = new HashSet<>();
        // Start crawling friends of the target user
        while (System.currentTimeMillis() - startTime < maxDuration) {
            List<WebElement> friendList = browser.findElements(By.xpath("//a[@tabindex='0' and contains(@href, 'facebook.com/')]"));
            String validUserRegex = "https://www\\.facebook\\.com/(?!profile\\.php\\?.*sk=|notifications|login_alerts|watch|events|stories|posts|videos|photos).+";
            Pattern pattern = Pattern.compile(validUserRegex);
            // Iterate through each friend link
            for (WebElement friend : friendList) {
                String url = friend.getAttribute("href");
                String name = friend.getText().trim();
                // Skip if the URL or name is empty or null
                if (url == null || name.isEmpty()) continue;
                // Check if UID not found. In this case, FB will respond with an exception page, containing a link to the help center. So I took advantage of it :>
                if (url.equals("https://www.facebook.com/help")) {
                    return null;
                }
                //Check regex
                Matcher matcher = pattern.matcher(url);
                if (matcher.matches() && !url.contains("/friends_mutual") && !url.contains("/friends") && !url.contains(profileUrl)) {
                    User user = new User();
                    user.setUsername(name);
                    user.setUrl(url);
                    friendSet.add(user);
                }
            }
            // Scroll down the page to load more friends.
            ((JavascriptExecutor) browser).executeScript("window.scrollTo(0, document.body.scrollHeight)");
            Thread.sleep(700);
            int newHeight = ((Long) ((JavascriptExecutor) browser).executeScript("return document.body.scrollHeight")).intValue();
            if (newHeight == lastHeight) {
                break;
            }
            lastHeight = newHeight;
        }
        return friendSet;
    }
}

/**
 * User class to store the username and URL of each friend.
 * Used to represent each friend with their associated Facebook URL.
 */
class User {
    private String username;
    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public String getUrl() {
        return url;
    }

    // Override equals and hashCode to ensure that users are unique based on their URL
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return Objects.equals(url, user.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}