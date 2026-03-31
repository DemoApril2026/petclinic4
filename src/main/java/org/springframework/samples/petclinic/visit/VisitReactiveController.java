package org.springframework.samples.petclinic.visit;

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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
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
 * Reactive CRUD operation (WEbFlux) for visit entity.
 *
 * @author Cedrick LUNVEN (@clunven)
 */
@RestController
@Validated
@RequestMapping("/petclinic/api/visits")
@CrossOrigin(
 methods = {PUT, POST, GET, OPTIONS, DELETE, PATCH},
 maxAge = 3600,
 allowedHeaders = {"x-requested-with", "origin", "content-type", "accept"},
 origins = "*"
)
@Tag(name = "Visit Api")
public class VisitReactiveController {
    
    /** Implementation of Crud operations for visits. */
    private VisitReactiveServices visitService;
   
    /**
     * Injection with controller
     */
    public VisitReactiveController(VisitReactiveServices visitService) {
        this.visitService = visitService;
    }
    
    /**
     * Read all pets from database.
     *
     * @return
     *   a {@link Flux} containing {@link PetEntity}
     */
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Read all visits in database")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of visits (even if empty)"), 
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Flux<Visit> findAllVisits() {
        return visitService.findAllVisits();
    }

    /**
     * Retrieve visit information by its unique identifier.
     *
     * @param visitId
     *      unique identifer as a String, to be converted in {@link UUID}.
     * @return
     *      a {@link Mono} of {@link Visit} or empty response with not found (404) code
     */
    @GetMapping(value = "/{visitId}", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieve visit information by its unique identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "the identifier exists and related visit is returned"), 
        @ApiResponse(responseCode = "400", description = "The uid was not a valid UUID"), 
        @ApiResponse(responseCode = "404", description = "the identifier does not exist in DB"),
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Visit>> findVisitById(
            @NotBlank
            @PathVariable("visitId") 
            @Parameter(required = true,example = "1ff2fbd9-bbb0-4cc1-ba37-61966aa7c5e6",
                       description = "Unique identifier of a visit")
            String visitId) {
        return visitService.findVisitById(visitId)
                           .map(ResponseEntity::ok)
                           .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * Create a {@link VisitEntity} when we don't know the visit identifier.
     *
     * @param request
     *      current http request
     * @param vetRequest
     *      fields required to create visit (no uid)
     * @return
     *      the created owner.
     */
    @PostMapping(produces = APPLICATION_JSON_VALUE, consumes=APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new visit, an unique identifier is generated and returned")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The visit has been created, uuid is provided in header"), 
        @ApiResponse(responseCode = "400", description = "The visit was malformed"),
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Visit>> createVisit(
            UriComponentsBuilder uc,
            @RequestBody @Valid Visit visit) {
        visit.setId(UUID.randomUUID());
        return visitService.createVisit(visit)
                           .map(created -> mapVisitAsHttpResponse(uc, created));
    }
    
    /**
     * Create or update a {@link VisitEntity}. We do not throw exception is already exist
     * or check existence as this is the behavirous in a cassandra table to read
     * before write.
     *
     * @param visitid
     *      unique identifier for visit
     * @param visit
     *      current visit
     * @return
     *      the created visit.
     */
    @PutMapping(value="/{visitId}",  
                consumes=APPLICATION_JSON_VALUE,
                produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Upsert a pet (no read before write as for Cassandra)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The visit has been created, uuid is provided in header"), 
        @ApiResponse(responseCode = "400", description = "The visit was malformed or uid was not valid"),
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Visit>> upsertVisit(
            UriComponentsBuilder uc, 
            @PathVariable("visitId") @NotBlank String visitId, 
            @RequestBody @Valid Visit visit) {
        Assert.isTrue(UUID.fromString(visitId).equals(visit.getId()), "Visit");
        return visitService.createVisit(visit).map(created -> mapVisitAsHttpResponse(uc, created));
    }
    
    /**
     * Delete a visit by its unique identifier.
     *
     * @param visitid
     *      visit identifier
     * @return
     */
    @DeleteMapping("/{visitId}")
    @Operation(summary = "Delete a visit by its unique identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "The pet has been deleted"), 
        @ApiResponse(responseCode = "400", description = "The uid was not a valid UUID"),
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Void>> deleteById(@PathVariable("visitId") @Parameter(
            required = true,example = "1ff2fbd9-bbb0-4cc1-ba37-61966aa7c5e6",
            description = "Unique identifier of a visit") @NotBlank String visitId) {
        return visitService.deleteVisitById(UUID.fromString(visitId))
                           .map(v -> new ResponseEntity<Void>(HttpStatus.NO_CONTENT));
    }
    
    /**
     * Syntaxic sugar to create the body when create new visit
     */
    private ResponseEntity<Visit> mapVisitAsHttpResponse(UriComponentsBuilder ucBuilder, Visit created) {
        return ResponseEntity.created(ucBuilder.path("/api/visits/{id}")
                        .buildAndExpand(created.getId().toString())
                        .toUri()).body(created);
    }
}
