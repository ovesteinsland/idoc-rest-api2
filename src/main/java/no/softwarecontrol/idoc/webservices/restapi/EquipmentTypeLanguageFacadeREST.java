package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.EquipmentType;
import no.softwarecontrol.idoc.data.entityobject.EquipmentTypeLanguage;
import no.softwarecontrol.idoc.data.entityobject.KeyFault;
import no.softwarecontrol.idoc.data.entityobject.KeyFaultLanguage;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.equipmenttypelanguage")
@RolesAllowed({"ApplicationRole"})
public class EquipmentTypeLanguageFacadeREST extends AbstractFacade<EquipmentTypeLanguage> {

    public EquipmentTypeLanguageFacadeREST() {
        super(EquipmentTypeLanguage.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "EquipmentTypeLanguage.findAll";
    }

    @POST
    @Path("create/{equipmentTypeId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("equipmentTypeId") String equipmentTypeId, EquipmentTypeLanguage entity) {
        EquipmentTypeFacadeREST equipmentTypeFacadeREST = new EquipmentTypeFacadeREST();
        EquipmentType equipmentType = equipmentTypeFacadeREST.find(equipmentTypeId);
        entity.setEquipmentType(equipmentType);
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, EquipmentTypeLanguage entity) {
        EquipmentTypeLanguage existing = this.find(entity.getEquipmentTypeLanguageId());
        if(existing != null) {
            existing.setName(entity.getName());
            existing.setDescription(entity.getDescription());
            existing.setLanguageCode(entity.getLanguageCode());
            existing.setLongName(entity.getLongName());
            existing.setNsNumber(entity.getNsNumber());
            super.edit(existing);
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public EquipmentTypeLanguage find(@PathParam("id") String id) {
        return super.find(id);
    }
}
