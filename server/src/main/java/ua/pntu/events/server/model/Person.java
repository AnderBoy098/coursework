package ua.pntu.events.server.model;

/**
 * Відповідальна особа.
 * Відповідає таблиці БД, див. database.sql.
 */
public class Person {

    private int id; // Ідентифікатор (PK)
    private String fullName; // ПІБ
    private String email; // Електронна пошта (унікальна)
    private String phone; // Телефон
    private String jobTitle; // Посада / роль

    public Person() {
    }

    public Person(int id, String fullName, String email, String phone, String jobTitle) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.jobTitle = jobTitle;
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

    @Override
    public String toString() {
        return "Person{id=" + id + ", fullName=" + fullName + ", email=" + email + ", phone=" + phone + "}";
    }
}
