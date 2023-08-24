package no.softwarecontrol.idoc.webservices.opplysningen1881;

import java.util.ArrayList;
import java.util.List;

public class Contacts {
    //private ExtensionData ExtensionData;
    private Geography geography;
    private List<ContactPoints> contactPoints = new ArrayList<>();
    public String id;
    public String firstName;
    public String lastName;
    private String type;

    // Read these from the ContactPoints and Addresses
    private String name;
    private String infoUrl;
    private String postCode;
    private String postArea;
    public Contacts() {

    }

    public Geography getGeography() {
        return geography;
    }

    public void setGeography(Geography geography) {
        this.geography = geography;
    }

    public List<ContactPoints> getContactPoints() {
        return contactPoints;
    }

    public void setContactPoints(List<ContactPoints> contactPoints) {
        this.contactPoints = contactPoints;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfoUrl() {
        return infoUrl;
    }

    public void setInfoUrl(String infoUrl) {
        this.infoUrl = infoUrl;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getPostArea() {
        return postArea;
    }

    public void setPostArea(String postArea) {
        this.postArea = postArea;
    }
}
