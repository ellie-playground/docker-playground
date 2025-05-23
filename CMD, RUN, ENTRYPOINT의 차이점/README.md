# RUN

- Docker 파일로부터 **Docker 이미지를 빌드하는 순간에 실행되는 명령어**
- 주로 라이브러리 설치 시 활용된다.

# CMD

- 이미지를 빌드할 때 실행되는 것과 달리, **이미지에서 컨테이너를 실행할 때 실행할 명령을 설정한다.**

# ENTRYPOINT

- `CMD` 명령어와 비슷하게 컨테이너가 생성되고 최초로 실행할 때 수행되는 명령어를 지정한다.

# CMD와 ENTRYPOINT의 차이

> `ENTRYPOINT`는 항상 실행되고, `CMD`는 docker run 명령 실행 시 변경 가능하다.
> 

공식 문서에 따르면, `CMD`와 `ENTRYPOINT`는 여러 개의 명령어를 나열할 경우, 가장 마지막의 `CMD`만 적용된다.

`CMD`의 목적은 실행 중인 컨테이너에 대한 기본 값을 제공하는 것이다. 그래서 만약 컨테이너 실행 시 명령어 인자를 주면, **인자의 실행 결과만 출력되고 Dockerfile 내부에 있는** `CMD`**는 실행되지 않는다.**

반면, `ENTRYPOINT`는 컨테이너가 시작될 때 필수적으로 특정 명령을 실행해야 하는 경우 적합하다. 실행 시 인자를 넘겨주더라도 기본적으로 실행되기 때문이다.

## CMD와 ENTRYPOINT 비교

```docker
FROM ubuntu:20.04

CMD ["echo", "cmd test"]
```

```docker
FROM ubuntu:20.04

ENTRYPOINT ["echo", "entrypoint test"]
```

### 실행 결과

```java
% docker run entrypoint_test echo hello                     
entrypoint test echo hello

% docker run cmd_test echo hello                     
hello
```

- `CMD` 명령어는 이미지 실행 시 추가로 `echo hello` 라는 커맨드를 주었을 때 `cmd test` 대신 `hello`가 출력된다.
    - 즉, 명령어가 오버라이딩되는 것을 확인할 수 있다.
    - `CMD`는 `docker run` 명령 내 **명시된 매개변수가 있는 경우 데몬에서 무시되기 때문이다.**
- 반면, `ENTRYPOINT`는 `entrypoint test`와 `hello`가 동시에 실행되는 것을 확인할 수 있다.
    - `ENTRYPOINT` 뒤로 넘겨주는 인자는 `ENTRYPOINT`의 인자를 덮어쓰지 않고, **하나의 인자 리스트로 전달되어 함께 실행되기 때문이다.**

## 여러 개의 CMD & 인자 넘겨주기

```docker
FROM ubuntu:20.04

CMD ["echo", "cmd test"]
CMD ["echo", "bye"]
CMD ["echo", "multiple cmd test"]
CMD ["ls", "-a"]
```

### 실행 결과

```docker
% docker run cmd_test echo hello       

hello

% docker run cmd_test ls        
bin
boot
dev
etc
home
lib
media
mnt
opt
proc
root
run
sbin
srv
sys
tmp
usr
var
```

- Docker 실행 시 여러개의 `CMD`를 사용하더라도 기존 CMD가 실행되지 않는 것을 확인할 수 있다.

## 여러 개의 CMD & 인자 없이 실행하기

```docker
FROM ubuntu:20.04

CMD ["echo", "cmd test"]
CMD ["echo", "bye"]
CMD ["echo", "multiple cmd test"]
#CMD ["ls", "-a"]
```

### 실행 결과

```docker
% docker run cmd_test                         
multiple cmd test
```

- 여러 개의 `CMD`를 사용할 경우, 인자를 넘겨주지 않으면 가장 마지막에 있는 `CMD`가 실행된다.

## 여러 개의 ENTRYPOINT & 인자 넘겨주기

```docker
FROM ubuntu:20.04

ENTRYPOINT ["echo", "entrypoint test"]
ENTRYPOINT ["echo", "entrypoint test2"]
ENTRYPOINT ["echo", "entrypoint test3"]
```

### 실행 결과

```docker
% docker run entrypoint_test echo bye
entrypoint test3 echo bye
```

- 가장 마지막에 사용한 `ENTRYPONT` 실행 결과와 인자로 넘겨준 값이 함께 실행된다.

## 여러 개의 ENTRYPOINT & 인자 없이 실행하기

```docker
FROM ubuntu:20.04

ENTRYPOINT ["echo", "entrypoint test"]
ENTRYPOINT ["echo", "entrypoint test2"]
ENTRYPOINT ["echo", "entrypoint test3"]
```

### 실행 결과

```docker
% docker run entrypoint_test                                
entrypoint test3
```

- 가장 마지막에 사용한 `ENTRYPOINT` 실행 결과가 출력된다.

# ENTRYPOINT와 CMD 응용하기

두 명령어를 같이 사용해 컨테이너 시작 작업을 자동화할 수 있다.

```docker
FROM ubuntu:20.04

ENTRYPOINT ["echo", "Hello, My name is"]
CMD ["Heejin"]
```

## 실행 결과

```docker
% docker run entrypoint_test                         
Hello, My name is Heejin
% docker run entrypoint_test Puppy  
Hello, My name is Puppy
```

- 항상 실행해야 하는 명령을 사용할 땐 `ENTRYPOINT`를 사용한다.
- `CMD`는 명시적으로 인자값을 지정하지 않는 경우, 기본 명령어 역할을 하는 인자를 설정하는데 사용한다.

# RUN과 CMD의 차이

공식 문서에 따르면, `RUN`은 명령어를 즉시 실행하고 그 결과를 이미지에 커밋한다.

반면, `CMD`는 이미지를 빌드할 때 사용하지 않으며, 컨테이너가 실행될 때 사용할 명령어를 지정한다.

# 참고 자료

- https://seokhyun2.tistory.com/61
- [https://velog.io/@inhalin/ENTRYPOINT-CMD-차이](https://velog.io/@inhalin/ENTRYPOINT-CMD-%EC%B0%A8%EC%9D%B4)
- https://docs.docker.com/reference/dockerfile/#entrypoint
- https://docs.docker.com/reference/dockerfile/#cmd