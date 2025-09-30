package no.softwarecontrol.idoc.webservices.data.requestdata;

import no.softwarecontrol.idoc.data.entityobject.Media;
import no.softwarecontrol.idoc.data.entityobject.Observation;

public class ObservationImage {
    private Observation observation;
    private Media media;

    public ObservationImage() {

    }

    public Observation getObservation() {
        return observation;
    }

    public void setObservation(Observation observation) {
        this.observation = observation;
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }
}
