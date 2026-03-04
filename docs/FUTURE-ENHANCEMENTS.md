# Бъдещи разширения на Quick Table

Този документ описва възможни разширения на системата за **събиране на допълнителни точки** в курсовия проект.

---

## 🎯 1. SOAP Протокол (+10 точки)

### Цел
Разработване на услуга, достъпна чрез различни комуникационни протоколи (REST + SOAP).

### Имплементация

#### 1.1 Добави SOAP Endpoint към Restaurant Service

**Dependency в pom.xml:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web-services</artifactId>
</dependency>
<dependency>
    <groupId>wsdl4j</groupId>
    <artifactId>wsdl4j</artifactId>
</dependency>
```

#### 1.2 Създай SOAP Config
```java
@Configuration
@EnableWs
public class WebServiceConfig extends WsConfigurerAdapter {
    
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext context) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(context);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "restaurants")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema schema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("RestaurantsPort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace("http://quicktable.com/restaurants");
        wsdl.setSchema(schema);
        return wsdl;
    }

    @Bean
    public XsdSchema restaurantsSchema() {
        return new SimpleXsdSchema(new ClassPathResource("restaurants.xsd"));
    }
}
```

#### 1.3 Създай restaurants.xsd
```xml
<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           targetNamespace="http://quicktable.com/restaurants"
           elementFormDefault="qualified">

    <xs:element name="getRestaurantRequest">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="id" type="xs:long"/>
            </xs:sequence>
        </xs:complexType>
    </xs:element>

    <xs:element name="getRestaurantResponse">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="restaurant" type="tns:restaurant"/>
            </xs:sequence>
        </xs:complexType>
    </xs:element>

    <xs:complexType name="restaurant">
        <xs:sequence>
            <xs:element name="id" type="xs:long"/>
            <xs:element name="name" type="xs:string"/>
            <xs:element name="address" type="xs:string"/>
            <xs:element name="latitude" type="xs:double"/>
            <xs:element name="longitude" type="xs:double"/>
        </xs:sequence>
    </xs:complexType>
</xs:schema>
```

#### 1.4 SOAP Endpoint
```java
@Endpoint
public class RestaurantSoapEndpoint {
    
    @Autowired
    private RestaurantService restaurantService;

    @PayloadRoot(namespace = "http://quicktable.com/restaurants", 
                 localPart = "getRestaurantRequest")
    @ResponsePayload
    public GetRestaurantResponse getRestaurant(@RequestPayload GetRestaurantRequest request) {
        RestaurantResponse restaurant = restaurantService.getRestaurantById(request.getId());
        
        GetRestaurantResponse response = new GetRestaurantResponse();
        // Map to SOAP response
        return response;
    }
}
```

#### 1.5 Тестване
```bash
# WSDL достъпен на:
http://localhost:8082/ws/restaurants.wsdl

# SOAP заявка:
curl -X POST http://localhost:8082/ws \
  -H "Content-Type: text/xml" \
  -d '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                        xmlns:rest="http://quicktable.com/restaurants">
        <soapenv:Body>
          <rest:getRestaurantRequest>
            <rest:id>1</rest:id>
          </rest:getRestaurantRequest>
        </soapenv:Body>
      </soapenv:Envelope>'
```

**Резултат:** Restaurant Service е достъпен чрез REST (port 8082) и SOAP (port 8082/ws) ✅

---

## 🖥️ 2. .NET Платформа (+10 точки)

### Цел
Разработване на услуга на различна платформа (.NET/C#).

### Имплементация: Notification Service на C#

#### 2.1 Създай .NET Web API проект
```bash
dotnet new webapi -n NotificationService
cd NotificationService
dotnet add package MailKit
```

#### 2.2 NotificationController.cs
```csharp
[ApiController]
[Route("api/[controller]")]
public class NotificationsController : ControllerBase
{
    [HttpPost("email")]
    public IActionResult SendEmail([FromBody] EmailRequest request)
    {
        // Изпрати email за потвърждение на резервация
        using var client = new SmtpClient();
        client.Connect("smtp.gmail.com", 587, false);
        
        var message = new MimeMessage();
        message.From.Add(new MailboxAddress("Quick Table", "noreply@quicktable.com"));
        message.To.Add(new MailboxAddress(request.CustomerName, request.Email));
        message.Subject = "Потвърждение на резервация";
        message.Body = new TextPart("plain") {
            Text = $"Здравейте {request.CustomerName},\n\n" +
                   $"Вашата резервация за {request.ReservationDate} е потвърдена!"
        };
        
        client.Send(message);
        client.Disconnect(true);
        
        return Ok(new { message = "Email изпратен успешно" });
    }
}
```

#### 2.3 Интегриране от Reservation Service
```java
// В ReservationService.java
public void sendConfirmationEmail(Reservation reservation) {
    WebClient client = webClientBuilder.baseUrl("http://localhost:5000").build();
    
    client.post()
        .uri("/api/notifications/email")
        .bodyValue(Map.of(
            "email", reservation.getCustomerEmail(),
            "customerName", reservation.getCustomerName(),
            "reservationDate", reservation.getReservationDate()
        ))
        .retrieve()
        .bodyToMono(String.class)
        .block();
}
```

**Резултат:** Java микросервизи + C# Notification Service ✅

---

## 3. BPEL Процес (+5-10 точки)

### Цел
Бизнес процес за автоматизация на резервационен workflow.

### Workflow:
```
1. Потребител създава резервация → PENDING
2. Автоматично изпращане на email до ресторанта
3. Ресторантът потвърждава → CONFIRMED
4. Изпращане на потвърждение до клиента
5. Ако няма отговор за 24 часа → AUTO-REJECT
```

### Инструменти:
- Apache ODE (BPEL Engine)
- Eclipse BPEL Designer

### BPEL Process (опростен):
```xml
<process name="ReservationWorkflow">
  <sequence>
    <receive operation="createReservation" variable="request"/>
    <invoke service="ReservationService" operation="create"/>
    <invoke service="NotificationService" operation="sendEmail"/>
    <wait for="'P1D'"/> <!-- Изчакай 24 часа -->
    <if>
      <condition>$status = 'PENDING'</condition>
      <invoke service="ReservationService" operation="reject"/>
    </if>
  </sequence>
</process>
```

---

## 4. API Gateway (Spring Cloud Gateway)

### Цел
Централизирана входна точка за всички микросервизи.

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/auth/**, /api/users/**
        
        - id: restaurant-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/restaurants/**
        
        - id: reservation-service
          uri: http://localhost:8083
          predicates:
            - Path=/api/reservations/**
```

**Предимства:**
- Един endpoint за клиенти: `http://localhost:8080`
- Централизирана JWT валидация
- Rate limiting
- Load balancing

---

## 5. Service Discovery (Eureka)

### Eureka Server
```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

### Client Configuration
```yaml
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
```

**Предимства:**
- Динамично откриване на услуги
- Автоматично load balancing
- Health checks

---

## 6. Docker Containerization

### Dockerfile (за всеки сервиз)
```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
  
  user-service:
    build: ./user-service
    ports:
      - "8081:8081"
    depends_on:
      - postgres
  
  restaurant-service:
    build: ./restaurant-service
    ports:
      - "8082:8082"
    depends_on:
      - postgres
```

**Стартиране:**
```bash
docker-compose up -d
```

---

## 7. Допълнителни външни услуги

### 7.1 Google Maps Places API
За по-добра геолокация и снимки на ресторантите.

```java
public RestaurantDetails getPlaceDetails(double lat, double lon) {
    String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
    // Call Google Maps API
}
```

### 7.2 SendGrid API
Професионални email уведомления.

```java
@Service
public class EmailService {
    @Value("${sendgrid.api-key}")
    private String apiKey;
    
    public void sendConfirmation(String email) {
        // Use SendGrid API
    }
}
```

### 7.3 Twilio SMS API
SMS уведомления за резервации.

---

## 📊 Обобщена таблица за точки

| Разширение | Сложност | Точки | Време |
|------------|----------|-------|-------|
| SOAP протокол | Средна | +10 | 4-6 часа |
| .NET сервиз | Средна | +10 | 6-8 часа |
| BPEL процес | Висока | +5-10 | 8-12 часа |
| API Gateway | Ниска | - | 2-3 часа |
| Docker | Ниска | - | 2-4 часа |
| Допълнителни API | Ниска | +2-5 | 1-2 часа |

**Препоръка:** Започни с SOAP и .NET за максимални точки (текущи 75 + 20 = 95).

---

## 🎓 Заключение

Текущият проект (75 точки) вече **преминава минимума (40 точки)** и е готов за защита. Разширенията са опционални, но силно препоръчителни за по-висока оценка.

**Приоритет:**
1. SOAP протокол → лесен, големи точки
2. .NET Notification Service → демонстрира мулти-платформа
3. Docker → улеснява демонстрацията

Успех! 🚀
