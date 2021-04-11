import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class SampleClient {

    public static void main(String[] theArgs) {

        // Create a FHIR client
        FhirContext fhirContext = FhirContext.forR4();
        IGenericClient client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        client.registerInterceptor(new LoggingInterceptor(false));

        // Search for Patient resources
		Bundle response = client
                .search()
                .forResource("Patient")
                .where(Patient.FAMILY.matches().value("SMITH"))
                .returnBundle(Bundle.class)
                .execute();

		// use the Bundle response to create list of FHIR Patient objects
        List<Patient> listPatients = new ArrayList<Patient>();
        response.getEntry().forEach(entry -> {
            listPatients.add((Patient) entry.getResource());
        });

        // String manipulation for printing
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

    }

}
