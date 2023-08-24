package no.softwarecontrol.idoc.webservices.opplysningen1881;

import no.softwarecontrol.idoc.webservices.opplysningen1881.Contacts;

import java.util.ArrayList;
import java.util.List;

public class Opplysningen1881Result {

//    {
//        "count": 1,
//            "contacts": [
//        {
//            "firstName": "Ove",
//                "lastName": "Steinsland",
//                "type": "Person",
//                "id": "696092S1",
//                "name": "Ove Steinsland",
//                "geography": {
//            "municipality": "Stord",
//                    "county": "Vestland",
//                    "region": "Vestlandet",
//                    "coordinate": null,
//                    "address": {
//                "street": "Berjahaugen",
//                        "houseNumber": "18",
//                        "entrance": "",
//                        "postCode": "5410",
//                        "postArea": "Sagvåg",
//                        "addressString": "Berjahaugen 18, 5410 Sagvåg"
//            }
//        },
//            "contactPoints": [
//            {
//                "label": "Mobiltelefon",
//                    "main": true,
//                    "type": "MobilePhone",
//                    "value": "41793713"
//            }
//            ],
//            "infoUrl": "https://www.1881.no/person/stord/sagvaag/ove-steinsland_696092S1/"
//        }
//    ]
//    }
    private Integer count;
    private List<Contacts> contacts = new ArrayList<>();
   // private List<Results> Results = new ArrayList<>();

    public Opplysningen1881Result(){

    }

    public List<Contacts> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contacts> contacts) {
        this.contacts = contacts;
    }
}
