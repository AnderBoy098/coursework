package ua.pntu.events.client.model;

/**
 * Відповідальна особа (клієнтська модель, відповідає JSON сервера).
 * Містить лише дані: без DAO, JDBC та прямого підключення до MySQL.
 */
public class Person {

    private int id;
    private String fullName;
    private String email;
    private String phone;
    private String jobTitle;

    public Person() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    /** Текст для ComboBox та повідомлень. */
    @Override
    public String toString() {
        return "#" + id + " " + fullName;
    }
}
