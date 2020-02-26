package se.sadmir.apigw;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@SpringBootApplication
@RestController
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@RequestMapping(method = RequestMethod.GET, path = "/employee-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<Employee> findAll() {
		
		
		// return getEmployeesAllAtOnce();
		// return getEmployeesStream();
		return getEmployeesIterable();
	}

	Flux<Employee> getEmployeesAllAtOnce() {
		return Flux.just(new Employee(100, "A", 10000), new Employee(200, "B", 20000), new Employee(300, "C", 30000));
	}

	Flux<Employee> getEmployeesStream() {
		Random r = new Random();
		return Flux.fromStream(Stream.generate(() -> r.nextInt()))
			.map(i -> new Employee(i, i + "", i *hashCode()))
			.doOnEach(e -> {
				System.out.println("Employee" + e);
			})
			.delayElements(Duration.ofSeconds(1))
			;
	}

	Flux<Employee> getEmployeesIterable() {
		List<Employee> emps = List.of(new Employee(100, "A", 10000), new Employee(200, "B", 20000), new Employee(300, "C", 30000));

		return Flux.fromIterable(emps)
			.doOnEach(e -> {
				System.out.println("Employee" + e);
			})
			.delayElements(Duration.ofSeconds(3))
			;
	}
	
	class Employee {
		public Employee(int i, String string, int j) {
			this.id = i;
			this.name = string;
			this.salary = j;
		}

		int id;
		String name;
		long salary;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public long getSalary() {
			return salary;
		}

		public void setSalary(long salary) {
			this.salary = salary;
		}

		
	}
}
