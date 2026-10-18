package ua.pntu.events.server.model;

/**
 * Ресурс.
 * Відповідає таблиці БД, див. database.sql.
 */
public class Resource {

    private int id; // Ідентифікатор (PK)
    private String name; // Назва ресурсу
    private String resourceType; // Тип ресурсу
    private int totalQuantity; // Загальна кількість
    private String unit; // Одиниця виміру

    public Resource() {
    }

    public Resource(int id, String name, String resourceType, int totalQuantity, String unit) {
        this.id = id;
        this.name = name;
        this.resourceType = resourceType;
        this.totalQuantity = totalQuantity;
        this.unit = unit;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    @Override
    public String toString() {
        return "Resource{id=" + id + ", name=" + name + ", resourceType=" + resourceType + ", totalQuantity=" + totalQuantity + "}";
    }
}
