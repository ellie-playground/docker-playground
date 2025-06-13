# 바인드 마운트(Bind mount)

호스트 시스템의 어느 곳에나 저장할 수 있고, **non-docker 프로세스나 도커 컨테이너 내에서 언제든지 수정 가능하다.**

바인드 마운트를 사용하면 **호스트 시스템의 파일 또는 디렉터리가 컨테이너에 마운트**되며, 파일 또는 디렉토리는 호스트의 전체 경로로 지정된다. 경로가 존재하지 않으면 생성해 마운트하고, 호스트 시스템의 경로에 모두 접근할 수 있기 때문에 주의가 필요하다.

만약, 우리가 마운트하고자 하는 컨테이너의 디렉터리 안에 파일이 존재한 상태로 마운트하게 되면, **기존에 존재한 내용은 전부 덮어 씌워진다.**

바인드 마운트는 호스트 머신의 파일 시스템 그 자체이기 때문에, Docker가 아닌 호스트 자체에서 관리하는 것이며, 바인드 마운트에서의 작업이 호스트 머신에도 영향을 끼친다.

## 사용 예시

### Docker

```docker
FROM ubuntu:20.04

RUN mkdir test && touch hello.txt
```

### docker-compose

```yaml
version: "3"
services:
  volume_test:
    build: .
    privileged: true
    volumes:
      - /Users/hee/Desktop/repository/docker-playground/Volume과 Bind Mount, 그리고 파일 시스템/test:/test
```

### 실행 결과

```bash
root@58067f5502d0:/# ls 
bin  boot  dev  etc  home  lib  media  mnt  opt  proc  root  run  sbin  srv  sys  test  tmp  usr  var
root@58067f5502d0:/# cd test
root@58067f5502d0:/test# ls
hello2.txt
```

위 코드로 Docker 컨테이너를 실행하면 내부에 hello.txt 파일이 사라지고, Host에 있는 hello2.txt가 생성되는데, 이는 **바인드 마운트를 통해 호스트 파일 시스템으로부터 디렉토리가 덮어 씌워졌기 때문이다.**

Host 경로에 파일을 생성하면 Docker 컨테이너 내부에도 파일이 생성된다.

### File System (MacOS)

```bash
root@58067f5502d0:/# df -h
Filesystem      Size  Used Avail Use% Mounted on
overlay          59G  5.3G   51G  10% /
tmpfs            64M     0   64M   0% /dev
shm              64M     0   64M   0% /dev/shm
grpcfuse        229G  207G   22G  91% /test
/dev/vda1        59G  5.3G   51G  10% /etc/hosts
```

```bash
root@58067f5502d0:/# df -T /test
Filesystem     Type          1K-blocks      Used Available Use% Mounted on
grpcfuse       fuse.grpcfuse 239362496 216365708  22996788  91% /test
```

기본적으로, Docker 컨테이너는 파일 시스템으로 overlay를 사용하고 있지만, 바인드 마운트를 통해 Host와 마운트된 파일 시스템은 grpcfuse로 표현되고 있다.

Docker Desktop이 호스트와 마운트할 때, **MacOS 호스트와 Linux 컨테이너 간 연결을 gRPC를 바탕으로 수행하기 때문이다.**

즉, **바인드 마운트를 이용해 폴더를 마운트하면, 해당 폴더의 파일 시스템이 Host 운영체제에 종속되는 것이다.**

# 볼륨(Volume)

만약 볼륨을 생성하게 되면, `/var/lib/docker/volume/~` 경로에 볼륨이 생성된 것을 확인할 수 있고, 해당 디렉터리는 **호스트 파일 시스템의 일부에 Docker에서 관리하는 영역에 저장된다.**

**non-docker 프로세스는 호스트 파일 시스템의 Docker 영역을 수정하면 안된다.**

볼륨은 여러 컨테이너에 동시에 탑재할 수 있고, 실행중인 컨테이너가 볼륨을 사용하지 않더라도 볼륨은 계속 Docker에서 사용할 수 있다.

## 사용 예시

### docker-compose

```yaml
version: "3"
services:
  volume_test:
    build: .
    privileged: true
    tty: true   # 컨테이너에 가상 터미널 할당
    stdin_open: true   # 표준 입력
    volumes:
      # 볼륨 - 볼륨 명:도커 경로
      - testvolume:/test
volumes:
  testvolume:
```

### 실행 결과

```bash
root@8f2e3d38ba07:/# ls
bin  boot  dev  etc  home  lib  media  mnt  opt  proc  root  run  sbin  srv  sys  test  tmp  usr  var
root@8f2e3d38ba07:/# cd test
root@8f2e3d38ba07:/test# ls
hello.txt
```

바인드 마운트와 달리, **Host의 영향을 받지 않기 때문에 Dockerfile에서 생성한 hello.txt 파일이 존재한다.**

### File System

```yaml
root@8f2e3d38ba07:/# df -h
Filesystem      Size  Used Avail Use% Mounted on
overlay          59G  5.3G   51G  10% /
tmpfs            64M     0   64M   0% /dev
shm              64M     0   64M   0% /dev/shm
/dev/vda1        59G  5.3G   51G  10% /test
```

```yaml
root@8f2e3d38ba07:/test# df -T /test
Filesystem     Type 1K-blocks    Used Available Use% Mounted on
/dev/vda1      ext4  61202244 5539508  52521412  10% /test
```

파일 시스템을 확인해보면, Host 파일 시스템이 마운트되지 않고, **Docker 컨테이너가 사용한 overlay 파일 시스템을 볼륨도 동일하게 사용하는 것을 확인할 수 있다.**

즉, Host OS에 종속되지 않고, Docker 자체적으로 해당 파일 시스템을 관리하는 것을 확인할 수 있다.

# Python os.rename과 shutil.move

파이썬에서 `os.rename` 함수와 `shutil.move` 함수 모두 특정 호스트 폴더에서 다른 폴더로 파일을 옮기는 함수이다.

하지만, `os.rename`의 경우 다른 파일 시스템으로 파일을 옮기는 것은 불가능하다.

따라서, 만약 **바인드 마운트된 폴더에서 Docker 컨테이너 내부로 `os.rename`으로 파일을 옮기면 오류가 발생한다.**

이때, `shutil.move`는 다른 파일 시스템으로도 파일을 옮길 수 있기 때문에 해당 함수를 사용하면 파일을 이동시킬 수 있다.

# 참고 자료

- https://ok-lab.tistory.com/121
- https://blog.naver.com/ncloud24/223105655768
- https://mycodings.fly.dev/blog/2024-04-06-docker-tutorial-understanding-bind-mount-and-port-publish
- [https://velog.io/@su5468/Docker-토막글-Bind-Mount와-파일-옮기기에-관한-여러분이-겪을-일-없을-문제](https://velog.io/@su5468/Docker-%ED%86%A0%EB%A7%89%EA%B8%80-Bind-Mount%EC%99%80-%ED%8C%8C%EC%9D%BC-%EC%98%AE%EA%B8%B0%EA%B8%B0%EC%97%90-%EA%B4%80%ED%95%9C-%EC%97%AC%EB%9F%AC%EB%B6%84%EC%9D%B4-%EA%B2%AA%EC%9D%84-%EC%9D%BC-%EC%97%86%EC%9D%84-%EB%AC%B8%EC%A0%9C)
- [https://velog.io/@shine230345/Docker-2.-설정volume-port-network-compose](https://velog.io/@shine230345/Docker-2.-%EC%84%A4%EC%A0%95volume-port-network-compose)
- [https://hobbylife.tistory.com/entry/파이썬-파일이동-osrename-shutilmove-차이와-사용-방법](https://hobbylife.tistory.com/entry/%ED%8C%8C%EC%9D%B4%EC%8D%AC-%ED%8C%8C%EC%9D%BC%EC%9D%B4%EB%8F%99-osrename-shutilmove-%EC%B0%A8%EC%9D%B4%EC%99%80-%EC%82%AC%EC%9A%A9-%EB%B0%A9%EB%B2%95)