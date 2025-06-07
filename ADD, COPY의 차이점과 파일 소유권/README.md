두 명령어 모두 특정 위치에서 Docker 이미지로 파일을 복사하는 기능을 수행한다.

# ADD

복사하려는 대상 파일이 압축 파일(`tar`, `tar.gz`)인 경우, 해당 파일의 압축을 해제해 복사한다.

`wget`을 이용해 원격지의 파일을 복사 대상으로 지정할 수 있다.

* 즉, 로컬 파일이나 디렉토리 대신 URL도 사용할 수 있다

## 실행 결과

```docker
FROM ubuntu:20.04

RUN mkdir copy_test
ADD test.tar.gz copy_test/
```

```docker
bin   copy_test  etc   lib    mnt  proc  run   srv  tmp  var
boot  dev        home  media  opt  root  sbin  sys  usr
root@f8251775866e:/# cd copy_test/
root@f8251775866e:/copy_test# ls
**test.txt  test2.txt     -> 압축 해제된 파일이 보인다.**
```

# COPY

host 환경의 파일 또는 디렉토리를 대상 컨테이너 이미지 안으로 복사한다.

로컬 파일 또는 디렉토리를 컨테이너에 복사하는 기능만 지원한다.

## 실행 결과

```docker
FROM ubuntu:20.04

RUN mkdir copy_test
COPY test.tar.gz copy_test/
```

```docker
root@3953f7d27681:/# ls 
bin   copy_test  etc   lib    mnt  proc  run   srv  tmp  var
boot  dev        home  media  opt  root  sbin  sys  usr
root@3953f7d27681:/# cd copy_test/
root@3953f7d27681:/copy_test# ls
**test.tar.gz      -> 압축이 해제되지 않고 단순히 복사만 되어있다.**
```

# ADD와 COPY의 차이점

`COPY`는 빌드 컨텍스트(Docker 빌드 시 접근 가능한 파일/폴더의 영역)나 멀티 스테이지 빌드의 다른 단계(`COPY —from=<stage name>`)에서 컨테이너로 파일을 복사할 때 사용하는 명령어이다.

반면, `ADD`는 원격 https 및 git URL에서 파일을 가져오거나, tar 파일을 자동으로 압축 해제해 추가하는 등의 기능을 지원한다.

공식 문서에서는 특별한 작업을 하지 않는다면 `COPY`를 추천하는데, 이는 `ADD`가 파일 복사 외에도 다운로드 시 파일 덮어쓰기 및 보안 취약성과 같은 위험이 따르기 때문이다.

## ADD와 COPY의 파일 권한

### COPY의 파일 권한

앞서 `COPY`한 파일의 소유자는 다음과 같다.

```docker
root@6a7d44bb04aa:/copy_test# ll
total 12
drwxr-xr-x 1 root root 4096 Jun  6 14:37 ./
drwxr-xr-x 1 root root 4096 Jun  7 08:32 ../
-rw-r--r-- 1 root root  228 Jun  6 14:27 test.tar.gz
```

`COPY`는 `ADD`와 달리 단순히 파일 자체를 복사하는데, 기본적으로 컨테이너 내 `root:root` 소유자로 복사된다.

> All files and directories copied from the build context are created with a UID and GID of `0` unless the optional `--chown` flag specifies a given username, groupname, or UID/GID combination to request specific ownership of the copied content.
> 

### ADD의 파일 권한

앞서 `ADD`한 파일의 소유자는 다음과 같다.

```docker
root@e2b148c034c2:/add_test# ll
total 16
drwxr-xr-x 1 root root    4096 Jun  6 14:53 ./
drwxr-xr-x 1 root root    4096 Jun  7 08:31 ../
-rw-r--r-- 1  501 dialout   48 Jun  6 14:26 test.txt
-rw-r--r-- 1  501 dialout   30 Jun  6 14:26 test2.txt
```

`ADD`로 파일을 복사할 경우, **Docker 내부에서 해당 파일의 권한은 호스트의 UID, 권한을 그대로 이미지에 반영한다. (정확히는 tar 아카이브 내부에 저장된 파일의 소유권을 사용한다.)**

예를 들어, Host에서 복사한 파일의 소유 권한이 root라면, 복사된 파일의 권한이 그대로 root로 들어간다.

아래와 같이 작성할 경우, 소유자를 지정할 수 있지만, **일반 파일에만 적용되고 압축 파일은 Host의 권한을 그대로 사용한다.**

```docker
FROM ubuntu:20.04

RUN mkdir add_test
RUN groupadd -r hello && useradd -r -g hello hello
ADD --chown=hello:hello test.tar.gz add_test/
```

```docker
root@849e719afa30:/add_test# ll
total 16
drwxr-xr-x 1 root root    4096 Jun  7 08:58 ./
drwxr-xr-x 1 root root    4096 Jun  7 08:58 ../
-rw-r--r-- 1  501 dialout   48 Jun  6 14:26 test.txt
-rw-r--r-- 1  501 dialout   30 Jun  6 14:26 test2.txt
```

```docker
root@ca35d2ff6370:/add_test# ll
total 12
drwxr-xr-x 1 root  root  4096 Jun  7 09:01 ./
drwxr-xr-x 1 root  root  4096 Jun  7 09:02 ../
-rw-r--r-- 1 hello hello   32 Jun  7 09:01 add_only.txt
```

그러나, 압축 파일이 아닌 일반 파일의 경우, `COPY`와 동일하게 `root:root` 권한으로 들어간다.

```docker
root@866a293e53a2:/add_test# ll
total 12
drwxr-xr-x 1 root root 4096 Jun  7 09:31 ./
drwxr-xr-x 1 root root 4096 Jun  7 09:32 ../
-rw-r--r-- 1 root root   32 Jun  7 09:01 add_only.txt
```

### 소유자 권한 변경하기

두 명령어를 통해 복사한 파일의 소유자를 변경하려면, 다음과 같은 명령어를 사용한다.

```docker
ADD [--chown=<user>:<group>] <src>...<dest>
ADD [--chown=<user>:<group>] ["<src>",..."<dest>"] (공백이 포함된 경로에 사용)

COPY [--chown=<user>:<group>] <src>...<dest>
COPY [--chown=<user>:<group>] ["<src>",..."<dest>"] (공백이 포함된 경로에 사용)
```

단, 사용 시 주의할 점이 있는데, `RUN`이나 `USER` 명령어를 통해 앞 레이어에서 사용자를 생성해야 정상적으로 지정된다.

**없는 사용자에 대해서는 `chown` 명령어가 적용되지 않는다.**

# 참고 자료

- https://parkgaebung.tistory.com/44
- https://docs.docker.com/build/building/best-practices/#add-or-copy
- https://malwareanalysis.tistory.com/233
- [https://www.tempmail.us.com/ko/dockerfile/dockerfile의-copy-와-add-명령-간의-차이점-이해](https://www.tempmail.us.com/ko/dockerfile/dockerfile%EC%9D%98-copy-%EC%99%80-add-%EB%AA%85%EB%A0%B9-%EA%B0%84%EC%9D%98-%EC%B0%A8%EC%9D%B4%EC%A0%90-%EC%9D%B4%ED%95%B4)
- https://github.com/moby/moby/issues/6119
- https://stackoverflow.com/questions/58493520/docker-add-chown-bug-or-feature