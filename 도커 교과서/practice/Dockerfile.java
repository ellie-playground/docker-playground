# 메이븐을 이용해 자바 애플리케이션을 빌드하는 Dockerfile 스크립트
# 기반 이미지는  diamol/maven이다. 이 이미지는 maven과 openjdk를 포함한다.
FROM diamol/maven AS builder
# builder 단계는 먼저 이미지에 작업 디렉터리를 만든 다음, 이 디렉터리에 pom.xml 파일을 복사한다.
WORKDIR /usr/src/iotd
# 이 파일에는 메이븐에서 수행할 빌드 절차가 정의돼 있다.
COPY pom.xml .
# 첫번째 RUN 인스트럭션에서 메이븐이 실행돼 필요한 의존 모듈을 내려 받는다. 이 과정에는 상당한 시간이 걸리기 때문에
# 별도의 단계로 분리해 레이어 캐시를 활용할 수 있도록 한다.
# 새로운 의존 모듈이 추가될 경우, XML 파일이 변경됐을 것이므로 이 단계가 다시 실행된다.
# 추가된 의존 모듈이 없다면 이미지 캐시를 재사용한다.
RUN mvn -B dependency:go-offline

# 그 다음 COPY . . 인스트럭션을 통해 나머지 소스 코드가 복사된다. 이 인스트럭션은
# 도커 빌드가 실행중인 디렉터리에 포함된 모든 파일과 서브 디렉터리를 현재 이미지 내 작업 디렉터리로 복사하라는 의미이다.
COPY . .
# builder 단계의 마지막은 mvn package 명령을 실행하는 것이다. 이 명령은 애플리케이션을 빌드하고 패키징하라는 의미다.
# 입력은 자바 소스코드이며, 출력은 JAR 포맷으로 패키징된 자바 애플리케이션이다.
RUN mvn package

# app
# 기반 이미지는 diamol/openjdk이다. 이 이미지는 자바 11 런타임을 포함하지만 메이븐은 포함하지 않는다.
FROM diamol/openjdk

# 이번에도 이미지에 작업 디렉터리를 만든다음, 여기에 앞서 builder 단계에서 만든 JAR 파일을 복사한다.
# 이 JAR 파일은 모든 의존 모듈과 컴파일된 애플리케이션을 포함하는 단일 파일이다. 그러므로 builder 단계의 파일 시스템에서 이 파일만 가져오면 된다.
WORKDIR /app
COPY --from=builder /usr/src/iotd/target/iotd-service-0.1.0.jar .
# 애플리케이션은 80번 포트를 주시하는 웹 서버 애플리케이션이다. 그러므로 이 포트를 EXPOSE 인스트럭션을 통해 외부로 공개해야 한다.
EXPOSE 80
# CMD 인스트럭션과 같은 기능을 하는 인스트럭션이다. 해당 이미지로 컨테이너가 실행되면 도커가 이 인스트럭션에 정의된 명령을 실행한다.
# 이 이미지의 경우 java 명령으로 빌드된 JAR 파일을 실행한다.
ENTRYPOINT ["java","-jar","iotd-service-0.1.0.jar"]