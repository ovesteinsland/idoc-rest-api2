package no.softwarecontrol.idoc.web.signup;

//import com.sendgrid.SendGrid;
//import com.sendgrid.SendGridException;
import no.softwarecontrol.idoc.authentication.PasswordAuthentication;
import no.softwarecontrol.idoc.data.entityhelper.CustomerData;
import no.softwarecontrol.idoc.data.entityhelper.ProjectNumber;
import no.softwarecontrol.idoc.data.entityhelper.ProjectParameters;
import no.softwarecontrol.idoc.data.entityobject.*;
import no.softwarecontrol.idoc.keysms.KeySmsController;
import no.softwarecontrol.idoc.restclient.IDocWebResource;
import no.softwarecontrol.idoc.webservices.restapi.*;
import org.joda.time.DateTime;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SignupTask {

    private final CustomerData customerData;
    private List<SignupTaskListener> listeners = new ArrayList<>();

    CompanyFacadeREST companyFacadeREST = new CompanyFacadeREST();
    UserFacadeREST userFacadeREST = new UserFacadeREST();
    AssetFacadeREST assetFacadeREST = new AssetFacadeREST();
    ProjectFacadeREST projectFacadeREST = new ProjectFacadeREST();
    ObservationFacadeREST observationFacadeREST = new ObservationFacadeREST();

    Company authority = null;
    Asset asset125A;
    Asset asset125B;
    Asset asset125C;
    Asset asset125D;

    User user;

    public SignupTask(CustomerData customerData) {
        this.customerData = customerData;
    }

    public void execute() {
        try {
            long start = System.currentTimeMillis();
            // Task 1
            // Create user

            fireProgress();
            Thread.sleep(500);

            System.out.println("Oppretter bruker");
            user = createUser();
            fireProgress();

            // Task 2
            // create authority
            System.out.println("Oppretter kontrollfirma");
            createAuthority();
            fireProgress();

            // Task 3
            // Link to user to authority
            System.out.println("Linker bruker til kontrollfirma");
            userFacadeREST.linkToCompany(authority.getCompanyId(),user);
            fireProgress();

            // Task 4
            // Link to Demo customer
            System.out.println("Oppretter demokunde");
            Company customer = createCustomer();
            fireProgress();

            // Task 5
            // Link to Software Control
            System.out.println("Linker til Software Control AS");
            linkToSoftwareControl();
            fireProgress();

            // Task 6
            AssetGroup assetGroup = createAssetGroup(customer);
            fireProgress();

            // Task 7-en
            // create assets on customer
            System.out.println("Oppretter bygning");
            asset125A = createAsset(assetGroup, customer, "Bolighuset", "Vikanesveien 125A");
            fireProgress();

            System.out.println("Oppretter plasseringer");
            createLocations();
            // Task 8
            // create assets on customer
            /*createAsset(assetGroup, customer, "Kårboligen", "Vikanesveien 125B");
            fireProgress();

            // Task 9
            // create assets on customer
            createAsset(assetGroup, customer, "Driftsbygningen", "Vikanesveien 125C");
            fireProgress();

            // Task 10
            // create assets on customer
            createAsset(assetGroup, customer, "Redskapsbygning", "Vikanesveien 125D");
            fireProgress();*/

            // Task 10
            // create project
            Location rom1 = asset125A.getLocationList().get(0).getLocationList().get(0);
            Location rom2 = asset125A.getLocationList().get(1).getLocationList().get(0);
            Location kjokken = asset125A.getLocationList().get(1).getLocationList().get(0);
            for(Location loc : asset125A.getLocationList().get(1).getLocationList() ){
                if(loc.getName().equalsIgnoreCase("Kjøkken")){
                    kjokken = loc;
                }
            }
            Location rom3 = asset125A.getLocationList().get(1).getLocationList().get(1);
            Location rom4 = asset125A.getLocationList().get(2).getLocationList().get(1);

            /*
            System.out.println("Oppretter kontroll");
            Project project = createProject(customer);

            System.out.println("Oppretter observasjon 1");
            Observation observation1 = createObservation(project,
                    "Fordeling. Merking var mangelfull og ikke i samsvar med sikringskurser/vern. FEL§32",
                    "NEK400:514 \n" +
                            "\n" +
                            "Kursfortegnelse ikke oppdatert etter ombygging. \n" +
                            "Gruppesikring 100A ikke merket. ",
                    1,
                    0,
                    2,
                    rom1);

            Observation observation2 = createObservation(project,
                    "Fastmontert stikkontakt i tak entre er forsynt fra lampepunkt med lampett ledning.",
                    "Stikkontakt er fast utstyr som skal ha permanent kabel iht. sikringsstørrelse.",
                    2,
                    0,
                    2,
                    rom2);

            Observation observation3 = createObservation(project,
                    "Stekeovn og ventilator er forsynt av sikring på 13A (kurs 9) med kabeltversnitt på 1,5mm2",
                    "Stikkontakt er fast utstyr som skal ha permanent kabel iht. sikringsstørrelse.",
                    3,
                    0,
                    2,
                    kjokken);

            Observation observation4 = createObservation(project,
                    "Stikkontakter og benkearmaturer er forsynt via skjøteledning, tilkoblet stikkontakt bak deksel i skap.",
                    "Dette er fast installasjon, som skal installeres som fast installasjon. ",
                    4,
                    0,
                    2,
                    kjokken);

            System.out.println("Oppretter observasjon for termografering");
            Observation observation10 = createObservation(project,
                    "Alvorlig termograferingsavvik funnet",
                    "",
                    5,
                    1,
                    3,
                    rom1);

            System.out.println("Oppretter termografi-målinger");
            createMeasurements(observation10);
            fireProgress();

            Observation observation11 = createObservation(project,
                    "Ingen termograferingsavvik funnet",
                    "OK",
                    6,
                    1,
                    0,
                    rom3);

            createMeasurements(observation11);
            fireProgress();

            Observation observation12 = createObservation(project,
                    "Ingen termograferingsavvik funnet",
                    "OK",
                    7,
                    1,
                    0,
                    rom3);

            createMeasurements(observation12);
            fireProgress();


            System.out.println("Oppretter observasjonsbilder");
            createObservationImage(observation1,"/resources/img/demo/observation/obs01_01.jpg",0);
            createObservationImage(observation1,"/resources/img/demo/observation/obs01_02.jpg",1);

            createObservationImage(observation2,"/resources/img/demo/observation/obs02_01.jpg",0);
            createObservationImage(observation2,"/resources/img/demo/observation/obs02_02.jpg",1);

            createObservationImage(observation3,"/resources/img/demo/observation/obs03_01.jpg",0);
            createObservationImage(observation3,"/resources/img/demo/observation/obs03_02.jpg",1);

            createObservationImage(observation4,"/resources/img/demo/observation/obs04_01.jpg",0);
            createObservationImage(observation4,"/resources/img/demo/observation/obs04_02.jpg",1);

            createObservationImage(observation10,"/resources/img/demo/observation/IR01.jpg",0);
            createObservationImage(observation10,"/resources/img/demo/observation/DC01.jpg",1);
            */
            sendToBernth(customerData);
            fireProgress();

            fireFinished();
            System.out.println("Ferdig");
            System.out.println(String.format("Opprettelse av ny kunde tok %d ms",System.currentTimeMillis() - start));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendToBernth(CustomerData customerData) {

        String message = String.format("%s %s\r\n",customerData.getFirstname(),customerData.getLastname());
        message += String.format("Mobil: %s\r\n",customerData.getMobile());
        message += String.format("E-post: %s\r\n",customerData.getEmail());
        message += String.format("Firma: %s\r\n",customerData.getCompany());

        Sms sms = new Sms();
        sms.setNumberList(new ArrayList<>());
        sms.getNumberList().add("41793713");
        sms.getNumberList().add("93044731");
        KeySmsController keySmsController = new KeySmsController();
        //String[] receivers = {"41793713"};
        String[] receivers = new String[sms.getNumberList().size()];
        for(int i=0; i< receivers.length; i++){
            receivers[i]=sms.getNumberList().get(i);
        }

        keySmsController.sendMessage(message, receivers);
    }

//    private void sendToBernth(CustomerData customerData) {
//
//
//        SendGrid sendgrid = new SendGrid("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");
//
//        SendGrid.Email email = new SendGrid.Email();
//        email.addTo("bernth.torsvik@idoc.no");
//        email.addTo("ove.steinsland@idoc.no");
//        email.setFrom("ove.steinsland@idoc.no");
//        email.setSubject("REGISTRERING: iDoc-bruker");
//        String message = String.format("%s %s\r\n",customerData.getFirstname(),customerData.getLastname());
//        message += String.format("Mobil: %s\r\n",customerData.getMobile());
//        message += String.format("E-post: %s\r\n",customerData.getEmail());
//        message += String.format("Firma: %s\r\n",customerData.getCompany());
//        email.setText(message);
//        try {
//            SendGrid.Response response = sendgrid.send(email);
//            System.out.println(response.getMessage());
//        } catch (SendGridException e) {
//            System.err.println(e);
//        }
//    }

    private void createMeasurements(Observation observation) {
        QuickChoiceItemFacadeREST quickChoiceItemFacadeREST = new QuickChoiceItemFacadeREST();
        QuickChoiceItem quickChoiceItem = quickChoiceItemFacadeREST.find("3859bd0e-0a93-4e20-8073-f379043b9898");
        for(Measurement measurement:quickChoiceItem.getMeasurementList()){
            Measurement duplicate = measurement.duplicate(false);
            observation.getMeasurementList().add(duplicate);
            duplicate.getObservationList().add(observation);
            if(duplicate.getName().equalsIgnoreCase("L1")){
                duplicate.setNumberValue(6.3);
            }
            if(duplicate.getName().equalsIgnoreCase("L2")){
                duplicate.setNumberValue(6.3);
            }
            if(duplicate.getName().equalsIgnoreCase("L3")){
                duplicate.setNumberValue(6.4);
            }
            if(duplicate.getName().equalsIgnoreCase("Mp1")){
                duplicate.setNumberValue(26.4);
            }
            if(duplicate.getName().equalsIgnoreCase("Mp2")){
                duplicate.setNumberValue(67.5);
            }
            if(duplicate.getName().equalsIgnoreCase("Diff")){
                duplicate.setNumberValue(41.1);
            }
        }
        observation.setQuickChoiceItem(quickChoiceItem);
        observation.setTitle(quickChoiceItem.getName());
        String locationId = "-1";
        String equipmentId = "-1";
        String quickChoiceItemId = quickChoiceItem.getQuickChoiceItemId();
        if(observation.getLocation() != null){
            locationId = observation.getLocation().getLocationId();
        }
        if(observation.getEquipment() != null){
            equipmentId = observation.getEquipment().getEquipmentId();
        }
        observationFacadeREST = new ObservationFacadeREST();
        observationFacadeREST.edit(
                observation.getObservationId(),
                locationId,
                quickChoiceItemId,
                equipmentId,
                observation);

    }

    private void createLocations() {

        Location etasje2 = new Location();
        etasje2.setLocationId(UUID.randomUUID().toString());
        etasje2.setName("2. Etasje");
        etasje2.setDeleted(false);
        etasje2.setEndItem(false);
        etasje2.setAsset(asset125A);
        asset125A.getLocationList().add(etasje2);

        createLocation(etasje2,"Soverom, nord",true);
        createLocation(etasje2,"Soverom, sør",true);
        createLocation(etasje2,"Soverom, vest",true);
        createLocation(etasje2,"Gang",true);


        Location etasje1 = new Location();
        etasje1.setLocationId(UUID.randomUUID().toString());
        etasje1.setName("1. Etasje");
        etasje1.setDeleted(false);
        etasje1.setEndItem(false);
        etasje1.setAsset(asset125A);
        asset125A.getLocationList().add(etasje1);

        createLocation(etasje1,"Entré",true);
        createLocation(etasje1,"Bad",true);
        createLocation(etasje1,"Kjøkken",true);
        createLocation(etasje1,"Stue",true);

        Location kjeller = new Location();
        kjeller.setLocationId(UUID.randomUUID().toString());
        kjeller.setName("Kjeller");
        kjeller.setDeleted(false);
        kjeller.setEndItem(false);
        kjeller.setAsset(asset125A);
        asset125A.getLocationList().add(kjeller);

        createLocation(kjeller,"Teknisk rom",true);
        createLocation(kjeller,"Vaskerom",true);
        createLocation(kjeller,"Bod",true);

        assetFacadeREST.edit(asset125A.getAssetId(),asset125A);
    }

    private void createLocation(Location parent, String name, boolean endItem) {
        Location rom = new Location();
        rom.setLocationId(UUID.randomUUID().toString());
        rom.setName(name);
        rom.setDeleted(false);
        rom.setEndItem(endItem);
        rom.setParent(parent);
        parent.getLocationList().add(rom);
    }


    public int getTaskCount() {
        return 12;
    }

    private Project createProject(Company customer) {
        //createWithParameters
        Project project = new Project();
        project.setProjectId(UUID.randomUUID().toString());
        ProjectNumber projectNumber = companyFacadeREST.incrementProjectCounter(authority.getCompanyId());
        project.setProjectNumber(projectNumber.getProjectCounter());
        project.setName(asset125A.getDefaultName());
        project.setGrouped(false);
        project.setDeleted(false);
        project.setProjectState(1);
        ProjectParameters parameters = new ProjectParameters();
        parameters.setProject(project);
        parameters.setAssetId(asset125A.getAssetId());
        parameters.setAuthorityId(authority.getCompanyId());
        parameters.setDisiplineId("bbe87718-1180-442d-90ef-84d31c328a32");
        parameters.setCustomerId(customer.getCompanyId());
        projectFacadeREST.createWithParameters(parameters);
        projectFacadeREST.linkToUser(user.getUserId(),project);

        return project;
    }

    private Observation createObservation(Project project,
                                   String description,
                                   String action,
                                   int observationNo,
                                   int observationType,
                                   int deviationGrade,
                                   Location location){
        Observation observation = new Observation();
        observation.setObservationId(UUID.randomUUID().toString());
        observation.setObservationNo(observationNo);
        observation.setObservationType(observationType);
        observation.setDeviation(deviationGrade);
        observation.setDescription(description);
        observation.setDeleted(false);
        observation.setInfrared(false);
        observation.setAction(action);
        observation.setLocation(location);
        observation.setCreatedUser(user.getLoginName());
        observation.setCreatedDate(new Date());
        observation.setModifiedDate(new Date());
        observation.setModifiedUser(user.getLoginName());
        observationFacadeREST = new ObservationFacadeREST();
        observationFacadeREST.createWithProject(project.getProjectId(),observation);
        return observation;
    }

    private AssetGroup createAssetGroup(Company customer){
        // create asset group
        AssetGroupFacadeREST assetGroupFacadeREST = new AssetGroupFacadeREST();
        AssetGroup assetGroup = new AssetGroup();
        assetGroup.setAssetGroupId(UUID.randomUUID().toString());
        assetGroup.setName("Bygninger");
        assetGroupFacadeREST.create(assetGroup);

        assetGroupFacadeREST.linkToCompany(customer.getCompanyId(),assetGroup);
        return assetGroup;
    }

    private Asset createAsset(AssetGroup assetGroup, Company customer, String name, String address) {
        try {
            Asset asset = new Asset();
            asset.setAssetId(UUID.randomUUID().toString());
            asset.setName(name);
            asset.setAddress(address);

            assetFacadeREST.createWithAssetGroup(assetGroup.getAssetGroupId(), asset);
            assetFacadeREST.linkCompany(customer.getCompanyId(), asset);
            assetFacadeREST.linkCompany(authority.getCompanyId(), asset);

            URL url = new URL(IDocWebResource.getRootUrl() + "/resources/img/demo/asset01.jpg");
            BufferedImage bufferedImage = ImageIO.read(url);
            BufferedImage scaledMediumImage = ImageLoader.getInstance().scaleImage(bufferedImage, ImageLoader.ImageSize.MEDIUM);
            BufferedImage scaledSmallImage = ImageLoader.getInstance().scaleImage(bufferedImage, ImageLoader.ImageSize.SMALL);

            Media iDocImage = ImageLoader.getInstance().addImage(scaledMediumImage, asset, "image/jpeg", false, authority.getCompanyId());
//            MediaStorage.upload(scaledMediumImage, "software-control/"+iDocImage.getUrlMedium());
//            MediaStorage.upload(scaledSmallImage, "software-control/"+iDocImage.getUrlSmall());

            return asset;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
//        } catch (GeneralSecurityException e) {
//            e.printStackTrace();
        }
        return null;

    }

    private void fireProgress() {
        for(SignupTaskListener listener:listeners) {
            listener.onProgress();
        }
    }

    private void fireFinished() {
        for(SignupTaskListener listener:listeners) {
            listener.onFinished();
        }
    }

    public void addListener(SignupTaskListener listener) {
        if(!listeners.contains(listener)){
            listeners.add(listener);
        }
    }

    private User createUser(){
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setFirstname(customerData.getFirstname());
        user.setLastname(customerData.getLastname());
        user.setEmail(customerData.getEmail());
        user.setLoginName(customerData.getEmail());
        user.setMobile(customerData.getMobile());

        PasswordAuthentication passwordAuthentication = new PasswordAuthentication(16);
        String passwordString = customerData.getPassword();
        String hashedPassword = passwordAuthentication.hash(passwordString.toCharArray());

        user.setPassword(hashedPassword);


        userFacadeREST.create(user);
        return user;
    }

    private void createAuthority(){
        authority = new Company();
        authority.setCompanyId(UUID.randomUUID().toString());
        String customerName = "Uten navn";
        if(!customerData.getCompany().isEmpty()) {
            customerName = customerData.getCompany();
        }
        authority.setName(customerName);
        authority.setCompanyType("AUTHORITY");
        authority.setDeleted(false);
        authority.setIsPersonCustomer(false);
        authority.setCreatedDate(new Date());

        DateTime expireDate = new DateTime();
        expireDate = expireDate.plusDays(31);
        authority.setExpireDate(expireDate.toDate());

        Company softwareControl = companyFacadeREST.find("a84bdb6d-6f4b-4116-985b-6418ade5e957");
        companyFacadeREST.create(authority);
        //companyFacadeREST.createWithContract(softwareControl.getCompanyId(),"AUTHORITY",authority);

        //  link til discipline
        // bbe87718-1180-442d-90ef-84d31c328a32 bolig
        // 84359e75-4857-44b5-9c66-de6509858e63 næring

        //companyFacadeREST.linkToDisipline("bbe87718-1180-442d-90ef-84d31c328a32",authority);
        //companyFacadeREST.linkToDisipline("84359e75-4857-44b5-9c66-de6509858e63",authority);

        companyFacadeREST.linkToDisipline("bbe87718-1180-442d-90ef-84d31c328a30",authority);
        companyFacadeREST.linkToDisipline("bbe87718-1180-442d-90ef-84d31c328a31",authority);
        companyFacadeREST.linkToDisipline("bbe87718-1180-442d-90ef-84d31c328a32",authority);
        companyFacadeREST.linkToDisipline("bbe87718-1180-442d-90ef-84d31c328a33",authority);
        companyFacadeREST.linkToDisipline("bbe87718-1180-442d-90ef-84d31c328a36",authority);
        companyFacadeREST.linkToDisipline("753b1507-c175-4a6e-9a53-da4564e8987b",authority);
        companyFacadeREST.linkToDisipline("bbe87718-1180-442d-90ef-84d31c328a37",authority);

    }

    private Company createCustomer(){
        try {
            Company customer = new Company();
            customer.setCompanyId(UUID.randomUUID().toString());
            customer.setOrganizationNumber(UUID.randomUUID().toString());
            customer.setName("DEMOKUNDE ANS");
            customer.setCompanyType("OWNER");
            customer.setDeleted(false);
            customer.setIsPersonCustomer(false);
            customer.setDemo(true);
            customer.setCreatedDate(new Date());

            companyFacadeREST.createWithContract(authority.getCompanyId(),"CUSTOMER",customer);

            URL url = new URL(IDocWebResource.getRootUrl() + "/resources/img/demo/demoLogo01.png");
            BufferedImage bufferedImage = ImageIO.read(url);
            BufferedImage scaledMediumImage = ImageLoader.getInstance().scaleImage(bufferedImage, ImageLoader.ImageSize.MEDIUM);
            BufferedImage scaledSmallImage = ImageLoader.getInstance().scaleImage(bufferedImage, ImageLoader.ImageSize.SMALL);

            Media iDocImage = ImageLoader.getInstance().addImage(scaledMediumImage, customer, "image/png", false, authority.getCompanyId());
//            MediaStorage.upload(scaledMediumImage, "software-control/"+iDocImage.getUrlMedium());
//            MediaStorage.upload(scaledSmallImage, "software-control/"+iDocImage.getUrlSmall());
            return customer;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
//        } catch (GeneralSecurityException e) {
//            e.printStackTrace();
        }
        return null;
    }

    private void linkToSoftwareControl(){
        Company softwareControl = companyFacadeREST.find("a84bdb6d-6f4b-4116-985b-6418ade5e957");
        ContractFacadeREST contractFacadeREST = new ContractFacadeREST();

        Contract contract = new Contract();
        contract.setContractId(UUID.randomUUID().toString());
        contract.setContractType("CUSTOMER");
        contract.setCompany(softwareControl);
        contract.setPartner(authority);
        contractFacadeREST.create(contract);

        /*if(!authority.getPartnerContracts().contains(contract)) {
            authority.getPartnerContracts().add(contract);
        }
        if(!softwareControl.getContractList().contains(contract)) {
            softwareControl.getContractList().add(contract);
        }
        companyFacadeREST.edit(softwareControl);
        companyFacadeREST.edit(authority);*/
    }



    public interface SignupTaskListener {
        void onProgress();
        void onFinished();
    }


}
