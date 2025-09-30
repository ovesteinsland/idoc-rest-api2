package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.*;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.measurementlanguage")
@RolesAllowed({"ApplicationRole"})
public class MeasurementLanguageFacadeREST extends AbstractFacade<MeasurementLanguage> {

    public MeasurementLanguageFacadeREST() {
        super(MeasurementLanguage.class);
    }

    @Override
    protected String getSelectAllQuery(){
        return "MeasurementLanguage.findAll";
    }

    @POST
    @Path("create/{measurementId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void create(@PathParam("measurementId") String measurementId, MeasurementLanguage entity) {
        MeasurementFacadeREST measurementFacadeREST = new MeasurementFacadeREST();
        Measurement measurement = measurementFacadeREST.find(measurementId);
        entity.setMeasurement(measurement);
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, MeasurementLanguage entity) {
        MeasurementLanguage existing = this.find(entity.getMeasurementLanguageId());
        if(existing != null) {
            existing.setUnit(entity.getUnit());
            existing.setName(entity.getName());
            existing.setDescription(entity.getDescription());
            existing.setLanguageCode(entity.getLanguageCode());
            existing.setTitleGroup(entity.getTitleGroup());
            existing.setMeasurementGroup(entity.getMeasurementGroup());
            super.edit(existing);
        }
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public MeasurementLanguage find(@PathParam("id") String id) {
        return super.find(id);
    }
}
