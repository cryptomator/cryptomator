//
//  CryptomatorDockTilePlugin.swift
//  Integrations
//
//  Created by Tobias Hagemann on 22.09.25.
//  Copyright © 2025 Cryptomator. All rights reserved.
//

import AppKit

class CryptomatorDockTilePlugin: NSObject, NSDockTilePlugIn {
	func setDockTile(_ dockTile: NSDockTile?) {
		guard let dockTile = dockTile, let image = Bundle(for: Self.self).image(forResource: "Cryptomator") else {
			return
		}
		dockTile.contentView = NSImageView(image: image)
		dockTile.display()
	}
}
