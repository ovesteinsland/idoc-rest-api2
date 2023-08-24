package no.softwarecontrol.idoc.webservices.restapi;


import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import no.softwarecontrol.idoc.data.entityhelper.QuickChoiceCache;
import no.softwarecontrol.idoc.data.entityobject.Disipline;
import no.softwarecontrol.idoc.data.entityobject.QuickChoiceGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@Path("no.softwarecontrol.idoc.entityobject.quickchoicecache")
@RolesAllowed({"ApplicationRole"})
public class QuickChoiceCacheFacadeREST {

    @Context
    ServletContext context;


    public QuickChoiceCacheFacadeREST() {

    }

    @PUT
    @Path("load/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public List<QuickChoiceCache> load(List<String> disiplineIds) {
        if (context.getAttribute("quickChoiceCacheList") == null) {
            build();
        }
        List<QuickChoiceCache> relevantQCCs = new ArrayList<>();
        for(String disiplineId: disiplineIds) {
            List<QuickChoiceCache> quickChoiceCacheList = (List<QuickChoiceCache>) context.getAttribute("quickChoiceCacheList");
            List<QuickChoiceCache> filteredList = quickChoiceCacheList
                    .stream()
                    .filter(p -> p.getDisiplineIds().contains(disiplineId))
                    .collect(Collectors.toList());
            for(QuickChoiceCache cache:filteredList) {
                if(!relevantQCCs.contains(cache)) {
                    relevantQCCs.add(cache);
                }
            }
        }
        return relevantQCCs;
    }

    @GET
    @Path("rebuild")
    //@Produces({MediaType.APPLICATION_JSON})
    public void rebuild() {
        build();
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<QuickChoiceCache> find(@PathParam("id") String id) {
        if (context.getAttribute("quickChoiceCacheList") != null) {
            List<QuickChoiceCache> quickChoiceCacheList = (List<QuickChoiceCache>) context.getAttribute("quickChoiceCacheList");
            return quickChoiceCacheList;
        } else {
            return build();
        }
    }

    private List<QuickChoiceCache> build() {
        QuickChoiceGroupFacadeREST quickChoiceGroupFacadeREST = new QuickChoiceGroupFacadeREST();
        List<QuickChoiceGroup> quickChoiceGroups = quickChoiceGroupFacadeREST.findRoot();
        List<QuickChoiceCache> quickChoiceCacheList = new ArrayList<>();
        for (QuickChoiceGroup quickChoiceGroup : quickChoiceGroups) {
            QuickChoiceCache quickChoiceCache = new QuickChoiceCache(quickChoiceGroup);
            for(Disipline disipline: quickChoiceGroup.getDisiplineList()) {
                quickChoiceCache.getDisiplineIds().add(disipline.getDisiplineId());
            }
            quickChoiceCacheList.add(quickChoiceCache);
        }

        context.setAttribute("quickChoiceCacheList", quickChoiceCacheList);
        return quickChoiceCacheList;
    }

}
