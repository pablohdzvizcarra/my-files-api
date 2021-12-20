package jvm.pablohdz.myfilesapi.api;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.URI;
import java.net.UnknownHostException;
import jvm.pablohdz.myfilesapi.dto.CSVFileDataDto;
import jvm.pablohdz.myfilesapi.dto.CSVFileDto;
import jvm.pablohdz.myfilesapi.entity.FileCSVData;
import jvm.pablohdz.myfilesapi.service.CSVService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping(value = "/api/files")
public class FileResource {
  private final CSVService csvService;
  private final Environment environment;

  @Autowired
  public FileResource(CSVService csvService, Environment environment) {
    this.csvService = csvService;
    this.environment = environment;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<CSVFileDto> uploadCSVFile(@RequestParam("file") MultipartFile file) {
    CSVFileDto dto = csvService.uploadFile(file);
    URI uri =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(dto.getId())
            .toUri();
    return ResponseEntity.created(uri).body(dto);
  }

  @GetMapping(value = "/{id}", produces = "text/csv")
  public ResponseEntity<InputStreamResource> readByID(@PathVariable("id") String id) {
    FileCSVData fileCSVData = csvService.downloadById(id);
    String csvFileName = fileCSVData.getFilename();
    InputStreamResource data = fileCSVData.getDataStreamResource();

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + csvFileName);
    httpHeaders.set(HttpHeaders.CONTENT_TYPE, "text/csv");
    return new ResponseEntity<>(data, httpHeaders, HttpStatus.OK);
  }

  @PutMapping(value = "/{id}", produces = "text/csv")
  public ResponseEntity<byte[]> update(
      @PathVariable("id") String id, @RequestParam("file") MultipartFile file) throws IOException {
    String uri = createURICreated(id);
    CSVFileDataDto data = csvService.update(id, file);
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + data.getFilename());
    httpHeaders.set(HttpHeaders.CONTENT_TYPE, data.getContentType());
    httpHeaders.set(HttpHeaders.LOCATION, uri);

    return new ResponseEntity<>(data.getData(), httpHeaders, HttpStatus.OK);
  }

  private String createURICreated(String id) throws UnknownHostException {
    return UriComponentsBuilder.newInstance()
        .scheme("http")
        .host(
            Inet6Address.getLocalHost().getHostAddress()
                + ":"
                + environment.getProperty("local.server.port"))
        .path("/api/files/" + id)
        .build()
        .toString();
  }
}
