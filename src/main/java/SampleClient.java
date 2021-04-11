import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.util.StopWatch;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class SampleClient {

    private static long totalMillis;

    public static void main(String[] theArgs) throws FileNotFoundException {

        StopWatch requestStopWatch = new StopWatch();
        // Create a FHIR client
        FhirContext fhirContext = FhirContext.forR4();
        IGenericClient client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        client.registerInterceptor(new LoggingInterceptor(false));
        client.registerInterceptor(new IClientInterceptor() {
            @Override
            public void interceptRequest(IHttpRequest theRequest) {
                requestStopWatch.startTask("task");
            }

            @Override
            public void interceptResponse(IHttpResponse theResponse) throws IOException {
                requestStopWatch.endCurrentTask();
                totalMillis = totalMillis + theResponse.getRequestStopWatch().getMillis();
            }
        });

        // Read names file in from resources folder
        for (int i=0; i < 3; i++) {
            totalMillis = 0;
            Scanner scanner = new Scanner(new File(SampleClient.class.getResource("names.txt").getFile()));

            Bundle response;
            List<Patient> listPatients = new ArrayList<Patient>();
            boolean hasNoCaching = false; // caching is on
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (i == 2) {
                    hasNoCaching = true;
                }
                // Search for Patient resources
                response = client
                        .search()
                        .forResource("Patient")
                        .where(Patient.FAMILY.matches().value(line))
                        .returnBundle(Bundle.class).cacheControl(new CacheControlDirective().setNoCache(hasNoCaching))
                        .execute();

                // use the Bundle response to create list of FHIR Patient objects
                response.getEntry().forEach(entry -> {
                    listPatients.add((Patient) entry.getResource());
                });
            }

            // String manipulation of patient data for printing
            List<String> printPatients = new ArrayList<String>();
            for (Patient pat : listPatients) {
                // Birth date printed in YYYY-MM-DD format
                DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd");
                String strBirthDate = "UNKNOWN";
                if (pat.getBirthDate() != null) {
                    strBirthDate = dateFormat.format(pat.getBirthDate());
                }
                // given name replacing "," by -
                String givenName = pat.getName().get(0).getGiven().get(0).getValueAsString().replaceFirst("\",\"", "-");
                // making sure first name always has a capital letter to start for sorting
                String strToAdd = givenName.replace(givenName.charAt(0), givenName.toUpperCase().substring(0, 1).charAt(0)) + " " + pat.getName().get(0).getFamily() + " " + strBirthDate;
                // making sure no duplicates added to printing list
                if (!printPatients.contains(strToAdd)) {
                    printPatients.add(strToAdd);
                }
            }
            // sorting
            printPatients.sort(Comparator.naturalOrder());
            // printing
            printPatients.forEach(System.out::println);

            System.out.println();
            String strDouble = String.format("%.3f", new Double(totalMillis).doubleValue() / 20 / 1000);
            System.out.println("Average response time " + strDouble + " seconds");
        }
    }

}
