package org.springframework.samples.petclinic.vet;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.OPTIONS;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive CRUD operation (WebFlux) for Vet entity.
 */
@RestController
@RequestMapping("/petclinic/api/vets")
@CrossOrigin(
 methods = {PUT, POST, GET, OPTIONS, DELETE, PATCH},
 maxAge = 3600,
 allowedHeaders = {"x-requested-with", "origin", "content-type", "accept"},
 origins = "*"
)
@Tag(name = "Veterinarians Api")
public class VetReactiveController {
    
    /** Logger for the class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(VetReactiveController.class);
    
    /** Implementatopn of Vet Services. */
    private VetReactiveServices vetServices; 
    
    /**
     * Inject service with controller
     */
    public VetReactiveController(VetReactiveServices vetServices) {
        this.vetServices = vetServices;
    }
    
    /**
     * Read all veterinarians from database.
     *
     * @return
     *   a {@link Flux} containing {@link VetEntity}
     */
    @PreAuthorize("hasRole(@roles.VET_ADMIN)" )
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Read all veterinarians in database")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of veterinarians"), 
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Flux<Vet> getAllVets() {
        LOGGER.info("Listing vets");
        return vetServices.findAllVets();
    }
    
    /**
     * Retrieve veterinarian information by its unique identifier.
     *
     * @param vetId
     *      unique identifier as a String, to be converted in {@link UUID}.
     * @return
     *      a {@link Mono} of {@link VetEntity} or empty response with not found (404) code
     */
    @GetMapping(value = "/{vetId}", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieve veterinarian information by its unique identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "the identifier exists and related veterinarian is returned"), 
        @ApiResponse(responseCode = "400", description = "The uid was not a valid UUID"), 
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Vet>> getVet(@PathVariable("vetId") @Parameter(
               required = true,example = "1ff2fbd9-bbb0-4cc1-ba37-61966aa7c5e6",
               description = "Unique identifier of a Veterinarian") String vetId) {
        LOGGER.info("Retrieve Vet infos from its id {}", vetId);
        return Mono.from(vetServices.findVetById(vetId))
                   .map(ResponseEntity::ok)
                   .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Create a {@link VetEntity} when we don't know the identifier.
     *
     * @param request
     *      current http request
     * @param vetRequest
     *      fields required to create Vet (no uid)
     * @return
     *      the create vet.
     */
    @PostMapping(produces = APPLICATION_JSON_VALUE, consumes=APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new veterinarian, an unique identifier is generated and returned")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The veterinarian has been created, uuid is provided in header"),
        @ApiResponse(responseCode = "400", description = "The uid was not a valid UUID"), 
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Vet>> createVet(UriComponentsBuilder uc, 
            @RequestBody @Valid Vet vet) {
        vet.setId(UUID.randomUUID());
        LOGGER.info("Creating Vet with generated id {}", vet.getId());
        return vetServices.createVet(vet)
                          .map(created -> mapVetAsHttpResponse(uc, created));
    }
    
    /**
     * Create or update a {@link VetEntity}. We do not throw an exception if the entity already exists
     * or check existence, as this would require a read before write, and Cassandra supports an
     * upsert style of interaction.
     *
     * @param request
     *      current http request
     * @param vetRequest
     *      fields required to create Vet (no uid)
     * @return
     *      the create vet.
     */
    @PutMapping(value="/{vetId}", produces = APPLICATION_JSON_VALUE, consumes=APPLICATION_JSON_VALUE)
    @Operation(summary = "Upsert a veterinarian (no read before write as for Cassandra)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The veterinarian has been created, uuid is provided in header"),
        @ApiResponse(responseCode = "400", description = "The vet bean was malformed or does not provide valid id"),
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Vet>> upsert(
            UriComponentsBuilder uc, 
            @PathVariable("vetId") String vetId, 
            @RequestBody @Valid Vet vet) {
      Assert.isTrue(UUID.fromString(vetId).equals(vet.getId()), "Vet identifier provided in vet does not match the value if path");
      vet.setId(UUID.fromString(vetId));
      LOGGER.info("Update Vet with id {}", vetId);
      return vetServices.createVet(vet)
                        .map(created -> mapVetAsHttpResponse(uc, created));
    }
    
    /**
     * Delete a veterinarian by its unique identifier.
     *
     * @param vetId
     *      veterinarian identifier
     * @return
     */
    @DeleteMapping("/{vetId}")
    @Operation(summary = "Delete a veterinarian by its unique identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "The veterinarian has been deleted"),
        @ApiResponse(responseCode = "400", description = "The uid was not a valid UUID"),
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Void>> deleteById(@PathVariable("vetId") @Parameter(
            required = true,example = "1ff2fbd9-bbb0-4cc1-ba37-61966aa7c5e6",
            description = "Unique identifier of a Veterinarian") @NotBlank String vetId) {
        LOGGER.info("Delete Vet with id {}", vetId);
        return vetServices.deleteVetById(vetId)
                          .map(v -> new ResponseEntity<Void>(HttpStatus.NO_CONTENT));
    }
    
    private ResponseEntity<Vet> mapVetAsHttpResponse(UriComponentsBuilder ucBuilder, Vet created) {
        return ResponseEntity.created(ucBuilder.path("/api/vets/{id}")
                        .buildAndExpand(created.getId().toString())
                        .toUri()).body(created);
    }
}
