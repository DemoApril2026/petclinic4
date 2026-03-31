package org.springframework.samples.petclinic.owner;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.OPTIONS;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.Objects;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * Reactive CRUD operations for Owners using Spring WebFlux
 */
@RestController
@RequestMapping("/petclinic/api/owners")
@Tag(name = "Owners Api")
@CrossOrigin(
 methods = {PUT, POST, GET, OPTIONS, DELETE, PATCH},
 maxAge = 3600, allowedHeaders = {"x-requested-with", "origin", "content-type", "accept"}, origins = "*"
)
public class OwnerReactiveController {
   
    /** Implementation of owner CRUD operations. */
    private OwnerReactiveServices ownerServices;
    
    /**
     * Injection with controller
     */
    public OwnerReactiveController(OwnerReactiveServices service) {
        this.ownerServices = service;
    }
   
    /**
     * Search owners by their lastName leveraging a secondary index.
     * The result having multiple outputs lead to use of Reactor object Flux<T>.
     * 
     * @param searchString
     *      input term from user
     * @return
     *      list of Owners matching the term
     */
    @GetMapping(value = "/*/lastname/{lastName}", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Search owner by their lastName")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of owners matching the lastname"), 
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Flux<Owner> searchOwnersByName(@PathVariable("lastName") String searchString) {
       return ownerServices.findOwnersByName(searchString);
    }
    
    /**
     * Read all owners from database.
     *
     * @return
     *   a {@link Flux} containing {@link VetEntity}
     */
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Read all owners in database")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of owners (even if empty)"), 
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Flux<Owner> findAllOwners() {
        return ownerServices.findAllOwners();     
    }
    
    /**
     * Retrieve owner information by its unique identifier.
     *
     * @param ownerId
     *      unique identifer as a String, to be converted in {@link UUID}.
     * @return
     *      a {@link Mono} of {@link OwnerEntity} or empty response with not found (404) code
     */
    @GetMapping(value = "/{ownerId}", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieve owner information by its unique identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "the identifier exists and related owner is returned"), 
        @ApiResponse(responseCode = "400", description = "The identifier was not a valid UUID"),
        @ApiResponse(responseCode = "404", description = "the identifier does not exist in DB"),
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Owner>> findOwner(@PathVariable("ownerId") @Parameter(
               required = true,example = "1ff2fbd9-bbb0-4cc1-ba37-61966aa7c5e6",
               description = "Unique identifier of an Owner") String ownerId) {
        return ownerServices.findOwnerById(ownerId)
                            .map(ResponseEntity::ok)
                            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * Create a {@link OwnerEntity} when we don't know the identifier.
     *
     * @param request
     *      current http request
     * @param vetRequest
     *      fields required to create Owner (no uid)
     * @return
     *      the created owner.
     */
    @PostMapping(produces = APPLICATION_JSON_VALUE, consumes=APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new owner, an unique identifier is generated and returned")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The owner has been created, uuid is provided in header"), 
        @ApiResponse(responseCode = "400", description = "The JSON body was not a valid JSON or does not match Owner structure"), 
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Owner>> createOwner(
            UriComponentsBuilder uc, @RequestBody Owner owner) {
      Objects.requireNonNull(owner);
      owner.setId(UUID.randomUUID());
      return ownerServices.createOwner(owner)
                          .map(created -> mapOwnerAsHttpResponse(uc,created));
    }
    
    /**
     * Create or update a {@link OwnerEntity}. We do not throw an exception if the entity already exists
     * or check existence, as this would require a read before write, and Cassandra supports an
     * upsert style of interaction.
     *
     * @param ownerId
     *      unique identifier for owner
     * @param owner
     *      person as owner
     * @return
     *      the created owner.
     */
    @PutMapping(value="/{ownerId}",  
                consumes=APPLICATION_JSON_VALUE,
                produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Upsert a owner (no read before write as for Cassandra)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "The owner has been created, uuid is provided in header"), 
        @ApiResponse(responseCode = "400", description = "The JSON body was malformed or does not match Owner structure"), 
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Owner>> upsertOwner(
            UriComponentsBuilder uc, 
            @PathVariable("ownerId") String ownerId, 
            @RequestBody @NotNull Owner owner) {
      return ownerServices.updateOwner(owner)
                          .map(created -> mapOwnerAsHttpResponse(uc, created));
    }
    
    /**
     * Delete a owner by its unique identifier.
     *
     * @param vetId
     *      veterinarian identifier
     * @return
     */
    @DeleteMapping("/{ownerId}")
    @Operation(summary = "Delete a owner by its unique identifier")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "The owner has been deleted"), 
        @ApiResponse(responseCode = "400", description = "The identifier was not a valid UUID"),
        @ApiResponse(responseCode = "500", description = "Internal technical error") })
    public Mono<ResponseEntity<Void>> deleteById(@PathVariable("ownerId") @Parameter(
            required = true,example = "1ff2fbd9-bbb0-4cc1-ba37-61966aa7c5e6",
            description = "Unique identifier of a owner") String ownerId) {
        return ownerServices.deleteOwner(ownerId)
                            .map(v -> new ResponseEntity<Void>(HttpStatus.NO_CONTENT));
    }
    
    /**
     * Create http response for entity creation (syntaxic sugar)
     */
    private ResponseEntity<Owner> mapOwnerAsHttpResponse(UriComponentsBuilder ucBuilder, Owner created) {
        return ResponseEntity.created(ucBuilder.path("/api/owners/{id}")
                        .buildAndExpand(created.getId().toString())
                        .toUri()).body(created);
    }
    
}
