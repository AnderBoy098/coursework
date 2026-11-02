package ua.pntu.events.client.model;

/**
 * Ресурс.
 * Містить лише дані: без DAO, JDBC та прямого підключення до MySQL.
 */
public class Resource {

    private int id;
    private String name;
    private String resourceType;
    private int totalQuantity;
    private String unit;

    public Resource() {
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

    /** Текст для ComboBox та повідомлень. */
    @Override
    public String toString() {
        return "#" + id + " " + name + " (" + totalQuantity + " " + unit + ")";
    }
}
