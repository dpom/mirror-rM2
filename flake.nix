{
  description = "Template for clojure projects";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.05";
    flake-parts.url = "github:hercules-ci/flake-parts";
    mynixpkgs.url = "git+https://github.com/dpom/mynixpkgs";
  };
  outputs = inputs @ { flake-parts, mynixpkgs, ... }:
    flake-parts.lib.mkFlake { inherit inputs;}
      {
        debug = true;
        systems = [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ];
        perSystem = { config, self', inputs', pkgs, system, ... }:
          let
            mypkgs = mynixpkgs.packages.${system};
          in
            {
              devShells.default = pkgs.mkShell {
                packages = [
                  pkgs.babashka
                  pkgs.clj-kondo
                  pkgs.clojure
                  pkgs.dbeaver-bin
                  pkgs.jdk22
                  mypkgs.cljstyle
                  pkgs.pandoc
                ];
              };
            };
        flake = {};
  };
}
        
