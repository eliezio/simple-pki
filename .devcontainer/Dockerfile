FROM public.ecr.aws/docker/library/docker:24-dind
RUN set -ex \
    && apk add --no-cache --update \
        zsh tmux vim curl git jq yq k9s skopeo \
        alpine-zsh-config zsh-theme-powerlevel10k \
        zsh-syntax-highlighting zsh-autosuggestions \
        sudo ttf-dejavu font-terminus font-inconsolata font-dejavu \
        font-noto font-noto-cjk font-awesome font-noto-extra \
        openjdk17-jdk py3-pip
RUN set -ex \
    && git clone https://github.com/powerline/fonts.git --depth=1 \
    && ( cd fonts; ./install.sh; ) \
    && rm -rf fonts
ARG MYUID=${MYUID:-1000}
RUN set -ex \
    && adduser -D -G root  -s /bin/zsh -u $MYUID vscode \
    && addgroup vscode wheel \
    && addgroup vscode docker \
    && sed -i '/root:x:0:0:root/ s/ash/zsh/g' /etc/passwd \
    && sed -i '/# %wheel ALL.*/s/^# //' /etc/sudoers
COPY zshrc /root/.zshrc
COPY tmux.conf /root/.tmux.conf
COPY --chown=vscode zshrc /home/vscode/.zshrc
COPY --chown=vscode tmux.conf /home/vscode/.tmux.conf
VOLUME /home/vscode
VOLUME /root
ENV LANG='C.UTF-8'
ENV LANGUAGE='en_US:en'
