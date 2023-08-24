package no.softwarecontrol.idoc.webservices.restapi;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityobject.Certificate;
import no.softwarecontrol.idoc.data.entityobject.Media;
import no.softwarecontrol.idoc.data.entityobject.User;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.certificate")
@RolesAllowed({"ApplicationRole"})
public class CertificateFacadeREST extends AbstractFacade<Certificate>  {

    public CertificateFacadeREST() {
        super(Certificate.class);
    }

    @Override
    protected String getSelectAllQuery() {
        return null;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON})
    @Override
    public void create(Certificate entity) {
        super.create(entity);
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void edit(@PathParam("id") String id, Certificate entity) {
        Certificate certificate = this.find(id);
        if (certificate != null) {
            certificate.setDeleted(entity.getDeleted());
            certificate.setAuthority(entity.getAuthority());
            certificate.setCertificateNumber(entity.getCertificateNumber());
            certificate.setDescription(entity.getDescription());
            certificate.setExpireDate(entity.getExpireDate());
            certificate.setIssuedDate(entity.getIssuedDate());
            certificate.setName(entity.getName());
            if(!entity.getImageList().isEmpty()) {
                for(Media media: entity.getImageList()) {
                    if(!certificate.getImageList().contains(media)) {
                        certificate.getImageList().add(media);
                    }
                }
            }

            super.edit(certificate);
        }
    }

    @PUT
    @Path("linkToUser/{certificateId}/{userId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public void linkToCompany(@PathParam("certificateId") String certificateId, @PathParam("userId") String userId) {
        UserFacadeREST userFacadeREST = new UserFacadeREST();
        User user = userFacadeREST.find(userId);
        Certificate certificate = this.find(certificateId);
        if (user != null && certificate != null) {
            if (!user.getCertificateList().contains(certificate)) {
                user.getCertificateList().add(certificate);
                userFacadeREST.edit(user);
            }
            if (!certificate.getUserList().contains(user)) {
                certificate.getUserList().add(user);
                this.edit(certificate);
            }
        }
    }
}
