package com.distelli.europa.tasks;

import com.distelli.europa.db.ContainerRepoDb;
import com.distelli.europa.db.PipelineDb;
import com.distelli.europa.db.RegistryManifestDb;
import com.distelli.europa.models.ContainerRepo;
import com.distelli.europa.models.PCCopyToRepository;
import com.distelli.europa.models.Pipeline;
import com.distelli.europa.models.PipelineComponent;
import com.distelli.europa.models.RawTaskEntry;
import com.distelli.europa.models.RegistryManifest;
import com.distelli.europa.pipeline.RunPipeline;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

@Log4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineTask implements Task {
    private static final ObjectMapper OM = new ObjectMapper();
    static {
        OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    private String domain;
    private String tag;
    private String containerRepoId;
    private String manifestId;
    private String pipelineId;
    private String startComponentId;
    private String destinationTag;

    public static final String ENTITY_TYPE = "pipe";

    @Override
    public RawTaskEntry toRawTaskEntry() {
        try {
            return RawTaskEntry.builder()
                .entityType(ENTITY_TYPE)
                .entityId(pipelineId)
                .lockIds(Collections.singleton(getLockId()))
                .privateTaskState(OM.writeValueAsBytes(this))
                .build();
        } catch ( RuntimeException ex ) {
            throw ex;
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        }
    }

    @JsonIgnore
    public String getLockId() {
        return domain + "/" + containerRepoId + "/" + tag;
    }

    public class Run implements Runnable {
        @Inject
        private PipelineDb _pipelineDb;
        @Inject
        private RunPipeline _runPipeline;
        @Inject
        private ContainerRepoDb _repoDb;
        @Inject
        private RegistryManifestDb _manifestDb;

        @Override
        public void run() {
            Pipeline pipeline = _pipelineDb.getPipeline(pipelineId);
            ContainerRepo repo = _repoDb.getRepo(domain, containerRepoId);
            String reference = tag;
            if ( null != manifestId ) {
                RegistryManifest manifest = _manifestDb.getManifestByRepoIdTag(domain, containerRepoId, tag);
                if ( null != manifest && ! manifestId.equals(manifest.getManifestId()) ) {
                    reference = manifestId;
                }
            }
            List<PipelineComponent> componentsToRun = pipeline.getComponents();
            if (null != startComponentId) {
                int startComponentIndex = pipeline.getComponentIndex(startComponentId).orElse(0);
                componentsToRun = componentsToRun.subList(startComponentIndex, componentsToRun.size());
            }
            if ((destinationTag != null) && (componentsToRun.get(0) instanceof PCCopyToRepository)) {
                ((PCCopyToRepository) componentsToRun.get(0)).setTag(destinationTag);
            }

            _runPipeline.runPipeline(componentsToRun, repo, reference, manifestId);
        }

    }

    public static class Factory implements TaskFactory {
        @Inject
        private Injector _injector;
        public PipelineTask toTask(RawTaskEntry entry) {
            try {
                return OM.readValue(entry.getPrivateTaskState(), PipelineTask.class);
            } catch ( RuntimeException ex ) {
                throw ex;
            } catch ( Exception ex ) {
                throw new RuntimeException(ex);
            }
        }
        @Override
        public Runnable toRunnable(RawTaskEntry entry) {
            Run run = toTask(entry).new Run();
            _injector.injectMembers(run);
            return run;
        }
    }
}
